// Copyright (c) Lefraudeur. All rights reserved.
// Original work: https://github.com/Lefraudeur/MujinaBaseV2

#pragma once

#include "logger/logger.hpp"

#ifdef _WIN32
	#include <Windows.h>
#elif defined(__linux__)
	#include <pthread.h>
#endif
#include <jni.h>
#include <string_view>
#include <type_traits>
#include <memory>
#include <vector>
#include <mutex>
#include <shared_mutex>
#include <cstdint>
#include <functional>


#define LOG_ERROR(exp, msg) if (!exp) logger::error(msg);

#define BEGIN_KLASS_DEF(unobf_klass_name, obf_klass_name) struct unobf_klass_name##_members; using unobf_klass_name = jni::klass<obf_klass_name, unobf_klass_name##_members>; struct unobf_klass_name##_members : public jni::empty_members	{ unobf_klass_name##_members(jclass owner_klass, jobject object_instance, bool is_global_ref) : jni::empty_members(owner_klass, object_instance, is_global_ref) {}

#define END_KLASS_DEF()	};

#define BEGIN_KLASS_DEF_EX(unobf_klass_name, obf_klass_name, inherit_from) struct unobf_klass_name##_members; using unobf_klass_name = jni::klass<obf_klass_name, unobf_klass_name##_members>; struct unobf_klass_name##_members : public inherit_from##_members { unobf_klass_name##_members(jclass owner_klass, jobject object_instance, bool is_global_ref) : inherit_from##_members(owner_klass, object_instance, is_global_ref) {}

#define KLASS_DECLARATION(unobf_klass_name, obf_klass_name) struct unobf_klass_name##_members; using unobf_klass_name = jni::klass<obf_klass_name, unobf_klass_name##_members>;
#define BEGIN_KLASS_MEMBERS_EX(unobf_klass_name, inherit_from) struct unobf_klass_name##_members : public inherit_from##_members { unobf_klass_name##_members(jclass owner_klass, jobject object_instance, bool is_global_ref) : inherit_from##_members(owner_klass, object_instance, is_global_ref) {}
#define BEGIN_KLASS_MEMBERS(unobf_klass_name) BEGIN_KLASS_MEMBERS_EX(unobf_klass_name, jni::empty)
#define END_KLASS_MEMBERS()	};

// Macro dispatch for JNI primitive types: (C++ type, JNI function prefix, JNI type suffix)
#define JNI_FOR_EACH_PRIMITIVE(M) \
    M(jboolean, Boolean, boolean) \
    M(jbyte, Byte, byte) \
    M(jchar, Char, char) \
    M(jshort, Short, short) \
    M(jint, Int, int) \
    M(jfloat, Float, float) \
    M(jlong, Long, long) \
    M(jdouble, Double, double)

namespace jni
{
	inline uint32_t _tls_index = 0;
	inline std::vector<jobject> _refs_to_delete{};
	inline std::mutex _refs_to_delete_mutex{};
	inline std::function<jclass(const char* class_name)> _custom_find_class{};

	inline JNIEnv* get_env()
	{
		if (!_tls_index) return nullptr;
#ifdef _WIN32
		return (JNIEnv*)TlsGetValue(_tls_index);
#elif __linux__
		return (JNIEnv*)pthread_getspecific(_tls_index);
#endif
	}
	inline void set_thread_env(JNIEnv* new_env)
	{
		if (get_env()) return;
#ifdef _WIN32
		TlsSetValue(_tls_index, new_env);
#elif __linux__
		pthread_setspecific(_tls_index, new_env);
#endif
	}

	inline bool init()
	{
		if (_tls_index) return true;
#ifdef _WIN32
		_tls_index = TlsAlloc();
#elif __linux__
		pthread_key_create(&_tls_index, nullptr);
#endif
		LOG_ERROR(_tls_index, "tls index allocation failed");
		if (!_tls_index)
			return false;
		return true;
	}
	inline void shutdown() //needs to be called on exit, library unusable after this
	{
		if (!get_env()) return;
		{
			std::lock_guard lock{ _refs_to_delete_mutex }; //shouldn't be necessary, every jni calls should be stopped before calling jni::destroy_cache
			for (jobject object : _refs_to_delete)
			{
				if (!object) continue;
				get_env()->DeleteGlobalRef(object);
			}
			_custom_find_class = {}; // destroy in case the custom find class stores a classloader reference
		}
		
#ifdef _WIN32
		TlsFree(_tls_index);
#elif __linux__
		pthread_key_delete(_tls_index);
#endif
	}

	inline void set_custom_find_class(std::function<jclass(const char* class_name)> find_class)
	{
		_custom_find_class = find_class;
	}

	template<size_t N>
	struct string_litteral
	{
		constexpr string_litteral(const char(&str)[N])
		{
			std::copy_n(str, N, value);
		}
		constexpr operator const char* () const
		{
			return value;
		}
		constexpr operator std::string_view() const
		{
			return value;
		}
		char value[N];
	};

	template<string_litteral... strs> inline constexpr auto concat()
	{
		constexpr std::size_t size = ((sizeof(strs.value) - 1) + ...); //-1 to not include null terminator (dumb)
		char concatenated[size + 1] = { '\0' }; //+1 for null terminator

		auto append = [i = 0, &concatenated](auto const& s) mutable
		{
			for (int n = 0; n < sizeof(s.value) - 1; ++n) concatenated[i++] = s.value[n]; //-1 to not include null terminator
		};
		(append(strs), ...);
		concatenated[size] = '\0';
		return string_litteral(concatenated);
	}

	template<string_litteral str> inline constexpr auto to_dot()
	{
		char new_str[sizeof(str.value)] = { '\0' };
		for (int i = 0; i < sizeof(str.value); ++i)
		{
			if (str.value[i] == '/') new_str[i] = '.';
			else new_str[i] = str.value[i];
		}
		return string_litteral(new_str);
	}

	template<typename klass_type> struct jclass_cache
	{
		inline static std::shared_mutex mutex{};
		inline static jclass value = nullptr;
	};

	template<typename klass_type> inline jclass get_cached_jclass() //findClass
	{
		JNIEnv* env = get_env();
		if (!env) return nullptr;
		jclass& cached = jclass_cache<klass_type>::value;
		{
			std::shared_lock shared_lock{ jclass_cache<klass_type>::mutex };
			if (cached) return cached;
		}
		jclass local = env->FindClass(klass_type::get_name());
		if (env->ExceptionCheck())
			env->ExceptionClear();
		jclass found = (jclass)env->NewGlobalRef(local);
		if (!found && _custom_find_class)
			found = (jclass)env->NewGlobalRef(_custom_find_class(klass_type::get_name()));
		LOG_ERROR(found, (const char*)(concat<"failed to find class: ", klass_type::get_name()>()));
		{
			std::unique_lock unique_lock{ jclass_cache<klass_type>::mutex };
			cached = found;
		}
		{
			std::lock_guard lock{ _refs_to_delete_mutex };
			_refs_to_delete.push_back(found);
		}
		return found;
	}


	class object_wrapper
	{
	public:
		object_wrapper(jobject object_instance, bool is_global_ref) :
			object_instance((is_global_ref && object_instance ? get_env()->NewGlobalRef(object_instance) : object_instance)),
			is_global_ref(is_global_ref)
		{
		}

		object_wrapper(const object_wrapper& other) :
			object_wrapper(other.object_instance, other.is_global_ref)
		{
		}

		virtual ~object_wrapper()
		{
			if (is_global_ref)
				clear_ref();
		}

		object_wrapper& operator=(const object_wrapper& other) //operator = keeps the current ref type
		{
			if (is_global_ref)
			{
				jobject old_instance = object_instance; // set before deleting, eg if operator= is called on itself or on an object_wrapper with the same object_instance
				object_instance = (other.object_instance ? get_env()->NewGlobalRef(other.object_instance) : nullptr);
				if (old_instance) get_env()->DeleteGlobalRef(old_instance);
			}
			else
				object_instance = other.object_instance;
			return *this;
		}

		bool operator==(const object_wrapper& other) const
		{
			return is_same_object(other);
		}

		bool is_same_object(const object_wrapper& other) const
		{
			return get_env()->IsSameObject(object_instance, other.object_instance) == JNI_TRUE;
		}

		template<typename klass_type>
		bool is_instance_of() const
		{
			return get_env()->IsInstanceOf(object_instance, get_cached_jclass<klass_type>()) == JNI_TRUE;
		}

		void clear_ref()
		{
			if (!object_instance) return;
			if (is_global_ref && get_env())
				get_env()->DeleteGlobalRef(object_instance);
			object_instance = nullptr;
		}

		operator jobject() const
		{
			return this->object_instance;
		}

		operator bool() const
		{
			return this->object_instance;
		}

		bool is_global() const
		{
			return is_global_ref;
		}

		jobject object_instance;
	private:
		bool is_global_ref; //global refs aren't destroyed on PopLocalFrame, and can be shared between threads
	};

	struct empty_members : public object_wrapper
	{
		empty_members(jclass owner_klass, jobject object_instance, bool is_global_ref) :
			object_wrapper(object_instance, is_global_ref),
			owner_klass(owner_klass)
		{
		}

		jclass owner_klass;
	};

	template<typename T, typename... U> inline constexpr bool is_any_of_type = (std::is_same_v<T, U> || ...);
	template<typename T> inline constexpr bool is_jni_primitive_type = is_any_of_type<T, jboolean, jbyte, jchar, jshort, jint, jfloat, jlong, jdouble>;

	enum is_static_t : bool
	{
		STATIC = true,
		NOT_STATIC = false
	};

	template<class T> inline constexpr auto get_signature_for_type()
	{
		if constexpr (std::is_void_v<T>) 
			return string_litteral("V");
		if constexpr (!is_jni_primitive_type<T> && !std::is_void_v<T>)
			return T::get_signature();
		if constexpr (std::is_same_v<jboolean, T>)
			return string_litteral("Z");
		if constexpr (std::is_same_v<jbyte, T>)
			return string_litteral("B");
		if constexpr (std::is_same_v<jchar, T>)
			return string_litteral("C");
		if constexpr (std::is_same_v<jshort, T>)
			return string_litteral("S");
		if constexpr (std::is_same_v<jint, T>)
			return string_litteral("I");
		if constexpr (std::is_same_v<jfloat, T>)
			return string_litteral("F");
		if constexpr (std::is_same_v<jlong, T>)
			return string_litteral("J");
		if constexpr (std::is_same_v<jdouble, T>)
			return string_litteral("D");
	}

	template<class array_element_type>
	class array : public object_wrapper
	{
	public:
		array(jobject object_instance, bool is_global_ref = false) :
			object_wrapper(object_instance, is_global_ref)
		{
		}

		array& operator=(const array& other) //operator= is not inherited by default
		{
			object_wrapper::operator=(other);
			return *this;
		}

		std::vector<array_element_type> to_vector() const
		{
			jsize length = get_length();
			std::vector<array_element_type> vector{};
			vector.reserve(length);
			if constexpr (!is_jni_primitive_type<array_element_type>)
			{
				for (jsize i = 0; i < length; ++i)
					vector.push_back( array_element_type(get_env()->GetObjectArrayElement((jobjectArray)object_instance, i)) );
			}
#define JNI_TO_VEC_CASE(TYPE, FUNC, TYPE2) \
			if constexpr (std::is_same_v<TYPE, array_element_type>) \
			{ \
				std::unique_ptr<TYPE[]> buffer = std::make_unique<TYPE[]>(length); \
				get_env()->Get##FUNC##ArrayRegion((j ## TYPE2 ## Array)object_instance, 0, length, buffer.get()); \
				vector.insert(vector.begin(), buffer.get(), buffer.get() + length); \
			}
			JNI_FOR_EACH_PRIMITIVE(JNI_TO_VEC_CASE)
#undef JNI_TO_VEC_CASE
			return vector;
		}

		jsize get_length() const
		{
			return get_env()->GetArrayLength((jarray)object_instance);
		}

		static constexpr auto get_signature()
		{
			return concat<"[", get_signature_for_type<array_element_type>()>();
		}

		static constexpr auto get_name() //this is used for FindClass
		{
			return get_signature();
		}

		static array create(const std::vector<array_element_type>& values)
		{
			jobject object = nullptr;
			if constexpr (!is_jni_primitive_type<array_element_type>)
			{
				object = get_env()->NewObjectArray((jsize)values.size(), get_cached_jclass<array_element_type>(), nullptr);
				for (jsize i = 0; i < values.size(); ++i)
					get_env()->SetObjectArrayElement((jobjectArray)object, i, (jobject)values[i]);
			}
#define JNI_CREATE_ARRAY_CASE(TYPE, FUNC, TYPE2) \
			if constexpr (std::is_same_v<TYPE, array_element_type>) \
			{ \
				object = get_env()->New##FUNC##Array((jsize)values.size()); \
				get_env()->Set##FUNC##ArrayRegion((j ## TYPE2 ## Array)object, 0, (jsize)values.size(), values.data()); \
			}
			JNI_FOR_EACH_PRIMITIVE(JNI_CREATE_ARRAY_CASE)
#undef JNI_CREATE_ARRAY_CASE
			return array(object);
		}
	};

	template<typename field_type, string_litteral field_name, is_static_t is_static = NOT_STATIC>
	class field
	{
	public:
		field(const empty_members& m) :
			m(m)
		{
			if (id) return;
			if (m.owner_klass)
			{
				if constexpr (is_static)
					id = get_env()->GetStaticFieldID(m.owner_klass, get_name(), get_signature());
				if constexpr (!is_static)
					id = get_env()->GetFieldID(m.owner_klass, get_name(), get_signature());
			}
			LOG_ERROR(id, (const char*)(concat<"failed to find fieldID: ", get_name(), " ", get_signature()>()));
		}

		field(const field& other) = delete; // make sure field won't be copied (we store a empty_members reference which must not be copied)

		field& operator=(const field_type& new_value)
		{
			set(new_value);
			return *this;
		}

		void set(const field_type& new_value)
		{
			if (!id || !m.owner_klass || (!is_static && !m.object_instance)) return;
			if constexpr (!is_jni_primitive_type<field_type>)
			{
				if constexpr (is_static)
					return get_env()->SetStaticObjectField(m.owner_klass, id, (jobject)new_value);
				if constexpr (!is_static)
					return get_env()->SetObjectField(m.object_instance, id, (jobject)new_value);
			}
#define JNI_SET_FIELD_CASE(TYPE, FUNC, TYPE2) \
			if constexpr (std::is_same_v<TYPE, field_type>) \
			{ \
				if constexpr (is_static) \
					return get_env()->SetStatic##FUNC##Field(m.owner_klass, id, new_value); \
				if constexpr (!is_static) \
					return get_env()->Set##FUNC##Field(m.object_instance, id, new_value); \
			}
			JNI_FOR_EACH_PRIMITIVE(JNI_SET_FIELD_CASE)
#undef JNI_SET_FIELD_CASE
		}

		auto get() const
		{
			if constexpr (!is_jni_primitive_type<field_type>)
			{
				if (!id || !m.owner_klass || (!is_static && !m.object_instance)) return field_type(nullptr);
				if constexpr (is_static)
					return field_type(get_env()->GetStaticObjectField(m.owner_klass, id));
				if constexpr (!is_static)
					return field_type(get_env()->GetObjectField(m.object_instance, id));
			}
#define JNI_GET_FIELD_CASE(TYPE, FUNC, TYPE2) \
			if constexpr (std::is_same_v<TYPE, field_type>) \
			{ \
				if (!id || !m.owner_klass || (!is_static && !m.object_instance)) return TYPE(0); \
				if constexpr (is_static) \
					return get_env()->GetStatic##FUNC##Field(m.owner_klass, id); \
				if constexpr (!is_static) \
					return get_env()->Get##FUNC##Field(m.object_instance, id); \
			}
			JNI_FOR_EACH_PRIMITIVE(JNI_GET_FIELD_CASE)
#undef JNI_GET_FIELD_CASE
		}

		operator field_type() const
		{
			return get();
		}

		static constexpr auto get_name()
		{
			return field_name;
		}

		static constexpr auto get_signature()
		{
			return get_signature_for_type<field_type>();
		}

		static constexpr bool is_field_static()
		{
			return is_static;
		}

		operator jfieldID() const
		{
			return id;
		}
	private:
		const empty_members& m;
		inline static jfieldID id = nullptr;
	};


	template<typename method_return_type, string_litteral method_name, is_static_t is_static = NOT_STATIC, class... method_parameters_type>
	class method
	{
	public:
		method(const empty_members& m) :
			m(m)
		{
			if (id) return;
			if (m.owner_klass)
			{
				if constexpr (is_static)
					id = get_env()->GetStaticMethodID(m.owner_klass, get_name(), get_signature());
				if constexpr (!is_static)
					id = get_env()->GetMethodID(m.owner_klass, get_name(), get_signature());
			}
			LOG_ERROR(id, (const char*)(concat<"failed to find methodID: ", get_name(), " ", get_signature()>()));
		}

		method(const method& other) = delete; // make sure method won't be copied (we store a empty_members reference which must not be copied)

		auto operator()(const method_parameters_type&... method_parameters) const
		{
			return call(method_parameters...);
		}

		auto call(const method_parameters_type&... method_parameters) const
		{
#define JNI_CONVERT_ARGS std::conditional_t<is_jni_primitive_type<method_parameters_type>, method_parameters_type, jobject>(method_parameters)...
			if constexpr (std::is_void_v<method_return_type>)
			{
				if (!id || !m.owner_klass || (!is_static && !m.object_instance)) return;
				if constexpr (is_static)
					get_env()->CallStaticVoidMethod(m.owner_klass, id, JNI_CONVERT_ARGS);
				if constexpr (!is_static)
					get_env()->CallVoidMethod(m.object_instance, id, JNI_CONVERT_ARGS);
				return;
			}

			if constexpr (!is_jni_primitive_type<method_return_type> && !std::is_void_v<method_return_type>)
			{
				if (!id || !m.owner_klass || (!is_static && !m.object_instance)) return method_return_type(nullptr);
				if constexpr (is_static)
					return method_return_type(get_env()->CallStaticObjectMethod(m.owner_klass, id, JNI_CONVERT_ARGS));
				if constexpr (!is_static)
					return method_return_type(get_env()->CallObjectMethod(m.object_instance, id, JNI_CONVERT_ARGS));
			}
#define JNI_CALL_METHOD_CASE(TYPE, FUNC, TYPE2) \
			if constexpr (std::is_same_v<TYPE, method_return_type>) \
			{ \
				if (!id || !m.owner_klass || (!is_static && !m.object_instance)) return TYPE(0); \
				if constexpr (is_static) \
					return get_env()->CallStatic##FUNC##Method(m.owner_klass, id, JNI_CONVERT_ARGS); \
				if constexpr (!is_static) \
					return get_env()->Call##FUNC##Method(m.object_instance, id, JNI_CONVERT_ARGS); \
			}
			JNI_FOR_EACH_PRIMITIVE(JNI_CALL_METHOD_CASE)
#undef JNI_CALL_METHOD_CASE
#undef JNI_CONVERT_ARGS
		}


		operator jmethodID() const
		{
			return id;
		}

		static constexpr auto get_name()
		{
			return method_name;
		}

		static constexpr auto get_signature()
		{
			return concat<"(", get_signature_for_type<method_parameters_type>()..., ")", get_signature_for_type<method_return_type>()>();
		}

		static constexpr bool is_method_static()
		{
			return is_static;
		}

	private:
		const empty_members& m;
		inline static jmethodID id;
	};


	template<class... method_parameters_type>
	using constructor = method<void, "<init>", jni::NOT_STATIC, method_parameters_type...>;


	template<string_litteral class_name, class members_type>
	class klass : public members_type
	{
	public:
		klass(jobject object_instance = nullptr, bool is_global_ref = false) :
			members_type(get_cached_jclass<klass>(), object_instance, is_global_ref) // be careful order of initialization matters
		{
		}

		klass(const klass& other) : klass(other.object_instance, other.is_global()) {} // very important to not copy jni::field and method

		klass& operator=(const klass& other) //operator= is not inherited by default
		{
			object_wrapper::operator=(other);
			return *this;
		}
		
		template<class... method_parameters_type>
		static klass new_object(jni::constructor<method_parameters_type...> members_type::*constructor, const method_parameters_type&... method_parameters) // tbh I was just playing with member pointers
		{
			klass tmp{}; //lmao
			return klass{jni::get_env()->NewObject(get_cached_jclass<klass>(), jmethodID(tmp.*constructor), std::conditional_t<is_jni_primitive_type<method_parameters_type>, method_parameters_type, jobject>(method_parameters)...)};
		}

		static constexpr auto get_name()
		{
			return class_name;
		}

		static constexpr auto get_signature()
		{
			return concat<"L", class_name, ";">();
		}
	};

	class frame
	{
	public:
		frame(jint capacity = 16)
		{
			get_env()->PushLocalFrame(capacity);
		}
		~frame()
		{
			get_env()->PopLocalFrame(nullptr);
		}
	};
}
