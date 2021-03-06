/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.core.impl;

import org.onosproject.core.IdBlock;
import org.onosproject.core.IdGenerator;
import org.onosproject.core.UnavailableIdException;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 * Base class of {@link IdGenerator} implementations which use {@link IdBlockAllocator} as
 * backend.
 */
public class BlockAllocatorBasedIdGenerator implements IdGenerator {
    protected final IdBlockAllocator allocator;
    protected Supplier<IdBlock> idBlock;

    /**
     * Constructs an ID generator which use {@link IdBlockAllocator} as backend.
     *
     * @param allocator the ID block allocator to use
     */
    protected BlockAllocatorBasedIdGenerator(IdBlockAllocator allocator) {
        this.allocator = allocator;
        this.idBlock = Suppliers.memoize(allocator::allocateUniqueIdBlock);
    }

    @Override
    public long getNewId() {
        try {
            return idBlock.get().getNextId();
        } catch (UnavailableIdException e) {
            synchronized (allocator) {
                idBlock = Suppliers.memoize(allocator::allocateUniqueIdBlock);
                return idBlock.get().getNextId();
            }
        }
    }
}
