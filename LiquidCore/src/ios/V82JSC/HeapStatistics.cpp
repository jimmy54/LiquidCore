/*
 * Copyright (c) 2018 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/LiquidCore for terms and conditions.
 */
#include "V82JSC.h"

using namespace v8;

HeapStatistics::HeapStatistics()
{
    total_heap_size_ = 0;
    total_heap_size_executable_ = 0;
    total_physical_size_ = 0;
    total_available_size_ = 0;
    used_heap_size_ = 0;
    heap_size_limit_ = 0;
    malloced_memory_ = 0;
    peak_malloced_memory_ = 0;
    does_zap_garbage_ = false;
}

HeapSpaceStatistics::HeapSpaceStatistics()
{
    NOT_IMPLEMENTED;
}

HeapObjectStatistics::HeapObjectStatistics()
{
    NOT_IMPLEMENTED;
}

HeapCodeStatistics::HeapCodeStatistics()
{
    NOT_IMPLEMENTED;
}

void HeapSnapshot::Delete()
{
    NOT_IMPLEMENTED;
}

void v8::HeapSnapshot::Serialize(v8::OutputStream*, v8::HeapSnapshot::SerializationFormat) const
{
    NOT_IMPLEMENTED;
}

void v8::HeapProfiler::AddBuildEmbedderGraphCallback(
    v8::HeapProfiler::BuildEmbedderGraphCallback callback, void* data)
{
    // Ignore
}

void v8::HeapProfiler::RemoveBuildEmbedderGraphCallback(
     v8::HeapProfiler::BuildEmbedderGraphCallback callback, void* data)
{
    // Ignore
}

const HeapSnapshot* v8::HeapProfiler::TakeHeapSnapshot(v8::ActivityControl*, v8::HeapProfiler::ObjectNameResolver*)
{
    NOT_IMPLEMENTED;
}
