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

import tachyon.worker.BlockStoreLocation;

/**
 * Interface for listening on meta data mutation methods of {@link TieredBlockStore}.
 */
public interface BlockMetaEventListener {

  void preCommitBlock(long userId, long blockId, BlockStoreLocation location);

  void postCommitBlock(long userId, long blockId, BlockStoreLocation location);

  void preMoveBlock(long userId, long blockId, BlockStoreLocation newLocation);

  void postMoveBlock(long userId, long blockId, BlockStoreLocation newLocation);

  void preRemoveBlock(long userId, long blockId);

  void postRemoveBlock(long userId, long blockId);
}