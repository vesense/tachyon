/*
 * Licensed to the University of California, Berkeley under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package tachyon.worker.block;

import java.io.IOException;
import java.util.List;

import com.google.common.base.Optional;

import tachyon.Users;
import tachyon.conf.TachyonConf;
import tachyon.thrift.FileDoesNotExistException;
import tachyon.thrift.OutOfSpaceException;
import tachyon.worker.BlockStoreLocation;
import tachyon.worker.block.io.BlockReader;
import tachyon.worker.block.io.BlockWriter;
import tachyon.worker.block.meta.BlockMeta;
import tachyon.worker.block.meta.TempBlockMeta;

/**
 * Class responsible for managing the Tachyon BlockStore and Under FileSystem. This class is
 * thread-safe.
 */
public class BlockDataManager {
  /** Block Store manager */
  private final BlockStore mBlockStore;
  /** Configuration values */
  private final TachyonConf mTachyonConf;

  /** User metadata, used to keep track of user heartbeats */
  private Users mUsers;

  /**
   * Creates a BlockDataManager based on the configuration values.
   * @param tachyonConf the configuration values to use
   */
  public BlockDataManager(TachyonConf tachyonConf) {
    mBlockStore = new TieredBlockStore();
    mTachyonConf = tachyonConf;
  }

  /**
   * Aborts the temporary block created by the user.
   * @param userId The id of the client
   * @param blockId The id of the block to be aborted
   * @return true if successful, false if unsuccessful
   * @throws IOException if the block does not exist
   */
  // TODO: This may be better as void
  public boolean abortBlock(long userId, long blockId) throws IOException {
    return mBlockStore.abortBlock(userId, blockId);
  }

  /**
   * Access the block for a given user. This should be called to update the evictor when necessary.
   *
   * @param userId The id of the client
   * @param blockId The id of the block to access
   */
  public void accessBlock(long userId, long blockId) {
    mBlockStore.accessBlock(userId, blockId);
  }

  /**
   * Cleans up after users, to prevent zombie users. This method is called periodically.
   */
  public void cleanupUsers() {
    for (long user : mUsers.getTimedOutUsers()) {
      mUsers.removeUser(user);
      mBlockStore.cleanupUser(user);
    }
  }

  /**
   * Commits a block to Tachyon managed space. The block must be temporary.
   * @param userId The id of the client
   * @param blockId The id of the block to commit
   * @return true if successful, false otherwise
   * @throws IOException if the block to commit does not exist
   */
  // TODO: This may be better as void
  public boolean commitBlock(long userId, long blockId) throws IOException {
    return mBlockStore.commitBlock(userId, blockId);
  }

  /**
   * Creates a block in Tachyon managed space. The block will be temporary until it is committed.
   *
   * @param userId The id of the client
   * @param blockId The id of the block to create
   * @param location The tier to place the new block in, -1 for any tier
   * @param initialBytes The initial amount of bytes to be allocated
   * @return A string representing the path to the local file
   * @throws IOException if the block already exists
   * @throws OutOfSpaceException if there is no more space to store the block
   */
  // TODO: We should avoid throwing IOException
  public String createBlock(long userId, long blockId, int location, long initialBytes)
      throws IOException, OutOfSpaceException {
    BlockStoreLocation loc = BlockStoreLocation.anyDirInTier(location);
    Optional<TempBlockMeta> optBlock =
        mBlockStore.createBlockMeta(userId, blockId, loc, initialBytes);
    if (optBlock.isPresent()) {
      return optBlock.get().getPath();
    }
    // Failed to allocate initial bytes
    throw new OutOfSpaceException("Failed to allocate " + initialBytes + " for user " + userId);
  }

  /**
   * Creates a block. This method is only called from a data server.
   * @param userId The id of the client
   * @param blockId The id of the block to be created
   * @param location The tier to create this block, -1 for any tier
   * @param initialBytes The initial amount of bytes to be allocated
   * @return the block writer for the local block file
   * @throws FileDoesNotExistException if the block is not on the worker
   * @throws IOException if the block writer cannot be obtained
   */
  // TODO: We should avoid throwing IOException
  public BlockWriter createBlockRemote(long userId, long blockId, int location, long initialBytes)
      throws FileDoesNotExistException, IOException {
    BlockStoreLocation loc = BlockStoreLocation.anyDirInTier(location);
    Optional<TempBlockMeta> optBlock =
        mBlockStore.createBlockMeta(userId, blockId, loc, initialBytes);
    if (optBlock.isPresent()) {
      Optional<BlockWriter> optWriter = mBlockStore.getBlockWriter(userId, blockId);
      if (optWriter.isPresent()) {
        return optWriter.get();
      }
      throw new IOException("Failed to obtain block writer.");
    }
    throw new FileDoesNotExistException("Block " + blockId + " does not exist on this worker.");
  }

  /**
   * Frees a block from Tachyon managed space.
   *
   * @param userId the id of the client
   * @param blockId the id of the block to be freed
   * @return true if successful, false otherwise
   * @throws IOException if an error occurs when removing the block
   */
  // TODO: This may be better as void, we should avoid throwing IOException
  public boolean freeBlock(long userId, long blockId) throws IOException {
    Optional<Long> optLock = mBlockStore.lockBlock(userId, blockId, BlockLock.BlockLockType.WRITE);
    if (!optLock.isPresent()) {
      return false;
    }
    Long lockId = optLock.get();
    mBlockStore.removeBlock(userId, blockId, lockId);
    mBlockStore.unlockBlock(lockId);
    return true;
  }

  /**
   * Gets a report for the periodic heartbeat to master. Contains the total used bytes on each
   * tier, blocks added since the last heart beat, and blocks removed since the last heartbeat.
   * @return a block heartbeat report
   */
  // TODO: Implement this
  public BlockHeartbeatReport getReport() {
    return null;
  }

  /**
   * Gets the metadata for the entire block store. Contains the block mapping per storage dir and
   * the total capacity and used capacity of each tier.
   * @return the block store metadata
   */
  public StoreMeta getStoreMeta() {
    return mBlockStore.getStoreMeta();
  }

  /**
   * Gets the temporary folder for the user in the under filesystem.
   * @param userId The id of the client
   * @return the path to the under filesystem temporary folder for the client
   */
  public String getUserUfsTmpFolder(long userId) {
    return mUsers.getUserUfsTempFolder(userId);
  }

  public long lockBlock(long userId, long blockId, int type) {
    // TODO: Define some conversion of int -> lock type
    Optional<Long> optLock = mBlockStore.lockBlock(userId, blockId, BlockLock.BlockLockType.WRITE);
    if (optLock.isPresent()) {
      return optLock.get();
    }
    // TODO: Decide on failure return value
    return -1;
  }

  public boolean moveBlock(long userId, long blockId, int tier) throws IOException {
    Optional<Long> optLock = mBlockStore.lockBlock(userId, blockId, BlockLock.BlockLockType.WRITE);
    // TODO: Define this behavior
    if (!optLock.isPresent()) {
      return false;
    }
    Long lockId = optLock.get();
    BlockStoreLocation dst = BlockStoreLocation.anyDirInTier(tier);
    boolean result = mBlockStore.moveBlock(userId, blockId, lockId, dst);
    mBlockStore.unlockBlock(lockId);
    return result;
  }

  public String readBlock(long userId, long blockId, long lockId) throws FileDoesNotExistException {
    Optional<BlockMeta> optBlock = mBlockStore.getBlockMeta(userId, blockId, lockId);
    if (optBlock.isPresent()) {
      return optBlock.get().getPath();
    }
    // Failed to find the block
    throw new FileDoesNotExistException("Block " + blockId + " does not exist on this worker.");
  }

  public BlockReader readBlockRemote(long userId, long blockId, long lockId)
      throws FileDoesNotExistException, IOException {
    Optional<BlockReader> optReader = mBlockStore.getBlockReader(userId, blockId, lockId);
    if (optReader.isPresent()) {
      return optReader.get();
    }
    throw new FileDoesNotExistException("Block " + blockId + " does not exist on this worker.");
  }

  public boolean requestSpace(long userId, long blockId, long bytesRequested) throws IOException {
    return mBlockStore.requestSpace(userId, blockId, bytesRequested);
  }

  public void setUsers(Users users) {
    mUsers = users;
  }

  public boolean unlockBlock(long lockId) {
    return mBlockStore.unlockBlock(lockId);
  }

  public boolean userHeartbeat(long userId, List<Long> metrics) {
    mUsers.userHeartbeat(userId);
    return true;
  }
}