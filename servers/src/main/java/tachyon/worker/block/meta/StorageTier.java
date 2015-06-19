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

package tachyon.worker.block.meta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Optional;

import tachyon.Constants;
import tachyon.conf.TachyonConf;
import tachyon.util.CommonUtils;

/**
 * Represents a tier of storage, for example memory or SSD. It serves as a container of
 * {@link StorageDir} which actually contains metadata information about blocks stored and space
 * used/available.
 * <p>
 * This class does not guarantee thread safety.
 */
public class StorageTier {
  private List<StorageDir> mDirs;
  private final int mTierAlias;

  public StorageTier(TachyonConf tachyonConf, int tierAlias) {
    mTierAlias = tierAlias;
    int level = tierAlias - 1;
    String tierDirPathConf =
        String.format(Constants.WORKER_TIERED_STORAGE_LEVEL_DIRS_PATH_FORMAT, level);
    String[] dirPaths = tachyonConf.get(tierDirPathConf, "/mnt/ramdisk").split(",");

    String tierDirCapacityConf =
        String.format(Constants.WORKER_TIERED_STORAGE_LEVEL_DIRS_QUOTA_FORMAT, level);
    String[] dirQuotas = tachyonConf.get(tierDirCapacityConf, "0").split(",");

    mDirs = new ArrayList<StorageDir>(dirPaths.length);

    for (int i = 0; i < dirPaths.length; i ++) {
      int index = i >= dirQuotas.length ? dirQuotas.length - 1 : i;
      long capacity = CommonUtils.parseSpaceSize(dirQuotas[index]);
      mDirs.add(new StorageDir(this, i, capacity, dirPaths[i]));
    }
  }

  public int getTierAlias() {
    return mTierAlias;
  }

  public long getCapacityBytes() {
    long capacityBytes = 0;
    for (StorageDir dir : mDirs) {
      capacityBytes += dir.getCapacityBytes();
    }
    return capacityBytes;
  }

  public long getAvailableBytes() {
    long availableBytes = 0;
    for (StorageDir dir : mDirs) {
      availableBytes += dir.getAvailableBytes();
    }
    return availableBytes;
  }

  public StorageDir getDir(int dirIndex) {
    return mDirs.get(dirIndex);
  }

  public Set<StorageDir> getStorageDirs() {
    return new HashSet<StorageDir>(mDirs);
  }

  public Optional<BlockMeta> getBlockMeta(long blockId) {
    for (StorageDir dir : mDirs) {
      Optional<BlockMeta> optionalBlock = dir.getBlockMeta(blockId);
      if (optionalBlock.isPresent()) {
        return optionalBlock;
      }
    }
    return Optional.absent();
  }
}