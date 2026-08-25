// Copyright (c) Lefraudeur. All rights reserved.
// Original work: https://github.com/Lefraudeur/MujinaBaseV2

#pragma once
#include "../jvmti/jvmti.hpp"

namespace transformer
{
	bool init(const jvmti& jvmti_instance, const maps::MemoryJarClassLoader& classLoader);
	void shutdown(const jvmti& jvmti_instance);
}