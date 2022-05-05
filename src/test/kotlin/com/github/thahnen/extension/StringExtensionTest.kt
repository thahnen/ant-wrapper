/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.thahnen.extension

import org.junit.Assert
import org.junit.Test


/**
 *  Unit tests on StringExtension
 */
open class StringExtensionTest {
    /** 1) Test on "String.getHash" method */
    @Test fun test_getHash() {
        Assert.assertEquals("ex4gcfq8qipgqe7yy9khs9e06", "abc/def.ghi".getHash())
    }


    /** 2) Test on "String.removeExtension" method */
    @Test fun test_removeExtension() {
        Assert.assertEquals("abc/def", "abc/def.ghi".removeExtension())
        Assert.assertEquals("abc/def", "abc/def".removeExtension())
    }


    /** 3) Test on "String.getFileName" method */
    @Test fun test_getFileName() {
        Assert.assertEquals("def.ghi", "abc/def.ghi".getFileName())
        Assert.assertEquals("def.ghi", "def.ghi".getFileName())
    }
}
