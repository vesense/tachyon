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

package tachyon.worker.block.io;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.io.Closer;

import tachyon.Constants;
import tachyon.worker.block.meta.BlockMeta;

/**
 * This class provides read access to a block data file locally stored in managed storage.
 * <p>
 * This class does not provide thread-safety. Corresponding lock must be acquired.
 */
public class LocalFileBlockReader implements BlockReader {
  private static final Logger LOG = LoggerFactory.getLogger(Constants.LOGGER_TYPE);
  private final BlockMeta mBlockMeta;
  private final String mFilePath;
  private final RandomAccessFile mLocalFile;
  private final FileChannel mLocalFileChannel;
  private final Closer mCloser = Closer.create();
  private final long mFileSize;

  public LocalFileBlockReader(BlockMeta blockMeta) throws IOException {
    mBlockMeta = Preconditions.checkNotNull(blockMeta);
    mFilePath = mBlockMeta.getPath();
    mLocalFile = mCloser.register(new RandomAccessFile(mFilePath, "r"));
    mLocalFileChannel = mCloser.register(mLocalFile.getChannel());
    mFileSize = mLocalFile.length();
  }

  @Override
  public ReadableByteChannel getChannel() {
    return mLocalFileChannel;
  }

  @Override
  public long getLength() {
    return mFileSize;
  }

  @Override
  public ByteBuffer read(long offset, long length) throws IOException {
    Preconditions.checkArgument(offset + length <= mFileSize,
        "offset=%s, length=%s, exceeds file size(%s)", offset, length, mFileSize);
    // TODO: May need to make sure length is an int
    if (length == -1) {
      length = mFileSize - offset;
    }
    return mLocalFileChannel.map(FileChannel.MapMode.READ_ONLY, offset, length);
  }

  @Override
  public void close() throws IOException {
    mCloser.close();
  }
}