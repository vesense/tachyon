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

package tachyon.worker.block.evictor;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import tachyon.Pair;
import tachyon.worker.BlockStoreLocation;
import tachyon.worker.block.BlockAccessEventListener;
import tachyon.worker.block.BlockMetadataManager;
import tachyon.worker.block.meta.BlockMeta;
import tachyon.worker.block.meta.StorageDir;
import tachyon.worker.block.meta.StorageTier;

/**
 * A simple evictor that evicts arbitrary blocks until the required size is met.
 */
public class NaiveEvictor implements Evictor, BlockAccessEventListener {
  private final BlockMetadataManager mMetaManager;

  public NaiveEvictor(BlockMetadataManager metadata) {
    mMetaManager = Preconditions.checkNotNull(metadata);
  }

  @Override
  public Optional<EvictionPlan> freeSpace(long availableBytes, BlockStoreLocation location) {
    List<Pair<Long, BlockStoreLocation>> toMove = new ArrayList<Pair<Long, BlockStoreLocation>>();
    List<Long> toEvict = new ArrayList<Long>();

    long freed = 0;
    long available = mMetaManager.getAvailableBytes(location);
    if (available >= availableBytes) {
      // The current space is sufficient, no need for eviction
      return Optional.of(new EvictionPlan(toMove, toEvict));
    }

    if (location.equals(BlockStoreLocation.anyTier())) {
      for (StorageTier tier : mMetaManager.getTiers()) {
        for (StorageDir dir : tier.getStorageDirs()) {
          for (BlockMeta block : dir.getBlocks()) {
            toEvict.add(block.getBlockId());
            freed += block.getBlockSize();
            if (available + freed >= availableBytes) {
              return Optional.of(new EvictionPlan(toMove, toEvict));
            }
          }
        }
      }
      return Optional.absent();
    }

    int tierAlias = location.tierAlias();
    StorageTier tier = mMetaManager.getTier(tierAlias);
    if (location.equals(BlockStoreLocation.anyDirInTier(tierAlias))) {
      // Loop over all dirs in the given tier
      for (StorageDir dir : tier.getStorageDirs()) {
        for (BlockMeta block : dir.getBlocks()) {
          toEvict.add(block.getBlockId());
          freed += block.getBlockSize();
          if (available + freed >= availableBytes) {
            return Optional.of(new EvictionPlan(toMove, toEvict));
          }
        }
      }
      return Optional.absent();
    }

    int dirIndex = location.dir();
    StorageDir dir = tier.getDir(dirIndex);
    for (BlockMeta block : dir.getBlocks()) {
      toEvict.add(block.getBlockId());
      freed += block.getBlockSize();
      if (available + freed >= availableBytes) {
        return Optional.of(new EvictionPlan(toMove, toEvict));
      }
    }
    return Optional.absent();
  }

  @Override
  public void onAccessBlock(long userId, long blockId) {
    // This naive evictor does nothing on block access
  }

}