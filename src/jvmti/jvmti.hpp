// Copyright (c) Lefraudeur. All rights reserved.
// Original work: https://github.com/Lefraudeur/MujinaBaseV2

#pragma once
#include <jvmti.h>
#include <string>
#include "../mappings.hpp"

// Requires jni to be attached first
class jvmti
{
public:
	jvmti(JavaVM* jvm);
	~jvmti();

	operator bool();

	// warning: ressource intensive
	maps::Class find_loaded_class(const char* class_name) const;
	std::string get_class_signature(const maps::Class& klass) const;
	maps::ClassLoader get_class_ClassLoader(const maps::Class& klass) const;

	jvmtiEnv* get_env() const;
private:
	jvmtiEnv* jvmti_env = nullptr;
};