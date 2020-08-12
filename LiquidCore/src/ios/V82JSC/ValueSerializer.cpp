/*
 * Copyright (c) 2018 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/LiquidCore for terms and conditions.
 */
#include "V82JSC.h"

using namespace v8;

/**
 * The embedder overrides this method to write some kind of host object, if
 * possible. If not, a suitable exception should be thrown and
 * Nothing<bool>() returned.
 */
Maybe<bool> ValueSerializer::Delegate::WriteHostObject(Isolate* isolate, Local<Object> object)
{
    NOT_IMPLEMENTED;
}

/**
 * Called when the ValueSerializer is going to serialize a
 * SharedArrayBuffer object. The embedder must return an ID for the
 * object, using the same ID if this SharedArrayBuffer has already been
 * serialized in this buffer. When deserializing, this ID will be passed to
 * ValueDeserializer::TransferSharedArrayBuffer as |transfer_id|.
 *
 * If the object cannot be serialized, an
 * exception should be thrown and Nothing<uint32_t>() returned.
 */
Maybe<uint32_t> ValueSerializer::Delegate::GetSharedArrayBufferId(Isolate* isolate,
                                                                  Local<SharedArrayBuffer> shared_array_buffer)
{
    NOT_IMPLEMENTED;
}

Maybe<uint32_t> ValueSerializer::Delegate::GetWasmModuleTransferId(Isolate* isolate,
                                                                   Local<WasmCompiledModule> module)
{
    NOT_IMPLEMENTED;
}

/**
 * Allocates memory for the buffer of at least the size provided. The actual
 * size (which may be greater or equal) is written to |actual_size|. If no
 * buffer has been allocated yet, nullptr will be provided.
 *
 * If the memory cannot be allocated, nullptr should be returned.
 * |actual_size| will be ignored. It is assumed that |old_buffer| is still
 * valid in this case and has not been modified.
 */
void* ValueSerializer::Delegate::ReallocateBufferMemory(void* old_buffer, size_t size,
                                     size_t* actual_size)
{
    NOT_IMPLEMENTED;
}

/**
 * Frees a buffer allocated with |ReallocateBufferMemory|.
 */
void ValueSerializer::Delegate::FreeBufferMemory(void* buffer)
{
    NOT_IMPLEMENTED;
}

ValueSerializer::ValueSerializer(Isolate* isolate)
{
    NOT_IMPLEMENTED;
}

ValueSerializer::ValueSerializer(Isolate* isolate, Delegate* delegate)
{
    NOT_IMPLEMENTED;
}
ValueSerializer::~ValueSerializer()
{
    NOT_IMPLEMENTED;
}

/**
 * Writes out a header, which includes the format version.
 */
void ValueSerializer::WriteHeader()
{
    NOT_IMPLEMENTED;
}

/**
 * Serializes a JavaScript value into the buffer.
 */
Maybe<bool> ValueSerializer::WriteValue(Local<Context> context, Local<Value> value)
{
    NOT_IMPLEMENTED;
}

/**
 * Returns the stored data (allocated using the delegate's
 * AllocateBufferMemory) and its size. This serializer should not be used once
 * the buffer is released. The contents are undefined if a previous write has
 * failed.
 */
std::pair<uint8_t*, size_t> ValueSerializer::Release()
{
    NOT_IMPLEMENTED;
}

/**
 * Marks an ArrayBuffer as havings its contents transferred out of band.
 * Pass the corresponding ArrayBuffer in the deserializing context to
 * ValueDeserializer::TransferArrayBuffer.
 */
void ValueSerializer::TransferArrayBuffer(uint32_t transfer_id,
                         Local<ArrayBuffer> array_buffer)
{
    NOT_IMPLEMENTED;
}

/**
 * Indicate whether to treat ArrayBufferView objects as host objects,
 * i.e. pass them to Delegate::WriteHostObject. This should not be
 * called when no Delegate was passed.
 *
 * The default is not to treat ArrayBufferViews as host objects.
 */
void ValueSerializer::SetTreatArrayBufferViewsAsHostObjects(bool mode)
{
    NOT_IMPLEMENTED;
}

/**
 * Write raw data in various common formats to the buffer.
 * Note that integer types are written in base-128 varint format, not with a
 * binary copy. For use during an override of Delegate::WriteHostObject.
 */
void ValueSerializer::WriteUint32(uint32_t value)
{
    NOT_IMPLEMENTED;
}
void ValueSerializer::WriteUint64(uint64_t value)
{
    NOT_IMPLEMENTED;
}
void ValueSerializer::WriteDouble(double value)
{
    NOT_IMPLEMENTED;
}
void ValueSerializer::WriteRawBytes(const void* source, size_t length)
{
    NOT_IMPLEMENTED;
}

/**
 * The embedder overrides this method to read some kind of host object, if
 * possible. If not, a suitable exception should be thrown and
 * MaybeLocal<Object>() returned.
 */
MaybeLocal<Object> ValueDeserializer::Delegate::ReadHostObject(Isolate* isolate)
{
    NOT_IMPLEMENTED;
}

/**
 * Get a WasmCompiledModule given a transfer_id previously provided
 * by ValueSerializer::GetWasmModuleTransferId
 */
MaybeLocal<WasmCompiledModule> ValueDeserializer::Delegate::GetWasmModuleFromId(
                                        Isolate* isolate, uint32_t transfer_id)
{
    NOT_IMPLEMENTED;
}

ValueDeserializer::ValueDeserializer(Isolate* isolate, const uint8_t* data, size_t size)
{
    NOT_IMPLEMENTED;
}
ValueDeserializer::ValueDeserializer(Isolate* isolate, const uint8_t* data, size_t size,
                  Delegate* delegate)
{
    NOT_IMPLEMENTED;
}
ValueDeserializer::~ValueDeserializer()
{
    NOT_IMPLEMENTED;
}

/**
 * Reads and validates a header (including the format version).
 * May, for example, reject an invalid or unsupported wire format.
 */
Maybe<bool> ValueDeserializer::ReadHeader(Local<Context> context)
{
    NOT_IMPLEMENTED;
}

/**
 * Deserializes a JavaScript value from the buffer.
 */
MaybeLocal<v8::Value> ValueDeserializer::ReadValue(Local<Context> context)
{
    NOT_IMPLEMENTED;
}

/**
 * Accepts the array buffer corresponding to the one passed previously to
 * ValueSerializer::TransferArrayBuffer.
 */
void ValueDeserializer::TransferArrayBuffer(uint32_t transfer_id,
                         Local<ArrayBuffer> array_buffer)
{
    NOT_IMPLEMENTED;
}

/**
 * Similar to TransferArrayBuffer, but for SharedArrayBuffer.
 * The id is not necessarily in the same namespace as unshared ArrayBuffer
 * objects.
 */
void ValueDeserializer::TransferSharedArrayBuffer(uint32_t id,
                               Local<SharedArrayBuffer> shared_array_buffer)
{
    NOT_IMPLEMENTED;
}

/**
 * Must be called before ReadHeader to enable support for reading the legacy
 * wire format (i.e., which predates this being shipped).
 *
 * Don't use this unless you need to read data written by previous versions of
 * blink::ScriptValueSerializer.
 */
void ValueDeserializer::SetSupportsLegacyWireFormat(bool supports_legacy_wire_format)
{
    NOT_IMPLEMENTED;
}

/**
 * Expect inline wasm in the data stream (rather than in-memory transfer)
 */
void ValueDeserializer::SetExpectInlineWasm(bool allow_inline_wasm)
{
    NOT_IMPLEMENTED;
}

/**
 * Reads the underlying wire format version. Likely mostly to be useful to
 * legacy code reading old wire format versions. Must be called after
 * ReadHeader.
 */
uint32_t ValueDeserializer::GetWireFormatVersion() const
{
    NOT_IMPLEMENTED;
}

/**
 * Reads raw data in various common formats to the buffer.
 * Note that integer types are read in base-128 varint format, not with a
 * binary copy. For use during an override of Delegate::ReadHostObject.
 */
bool ValueDeserializer::ReadUint32(uint32_t* value)
{
    assert(0);
    return false;
}
bool ValueDeserializer::ReadUint64(uint64_t* value)
{
    assert(0);
    return false;
}
bool ValueDeserializer::ReadDouble(double* value)
{
    assert(0);
    return false;
}
bool ValueDeserializer::ReadRawBytes(size_t length, const void** data)
{
    assert(0);
    return false;
}
MaybeLocal<SharedArrayBuffer> ValueDeserializer::Delegate::GetSharedArrayBufferFromId(
    Isolate* isolate, uint32_t clone_id)
{
    assert(0);
}
