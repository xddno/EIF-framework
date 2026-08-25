// Copyright (c) Lefraudeur. All rights reserved.
// Original work: https://github.com/Lefraudeur/MujinaBaseV2

#include "logger.hpp"

static std::ofstream logfile;

bool logger::init()
{
	logfile = std::ofstream("mujina_logs.txt");
	if (!logfile)
		return false;
	return true;
}

void logger::shutdown()
{
	logfile = std::ofstream();
}

void logger::log(std::string_view msg)
{
	logfile << "[Ember AC] info: " << msg << std::endl;
}

void logger::error(std::string_view msg)
{
	logfile << "[Ember AC] error: " << msg << std::endl;
}
