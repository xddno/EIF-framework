// Copyright (c) Lefraudeur. All rights reserved.
// Original work: https://github.com/Lefraudeur/MujinaBaseV2

#pragma once
#include <fstream>
#include <string_view>

namespace logger
{
	bool init();
	void shutdown();

	void log(std::string_view msg);
	void error(std::string_view msg);
}