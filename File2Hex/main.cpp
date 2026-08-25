// Copyright (c) Lefraudeur. All rights reserved.
// Original work: https://github.com/Lefraudeur/MujinaBaseV2

#include <iostream>
#include <fstream>
#include <filesystem>
#include <string>
#include <ios>


int main(int argc, const char* argv[])
{
	std::filesystem::path current_dir{ std::filesystem::current_path() };

	for (const std::filesystem::directory_entry& file : std::filesystem::directory_iterator(current_dir))
	{
		if (file.is_directory() 
			|| file.path().extension() == ".hpp" 
			|| file.path().filename().string().ends_with(argv[0])
			|| file.path().filename().string().starts_with("ignore_"))
		{
			continue;
		}

		std::ifstream bin(file.path(), std::ios::binary | std::ios::ate);
		if (!bin)
			continue;
		size_t bin_size = bin.tellg();
		bin.seekg(0, std::ios::beg);
		
		std::ofstream hex(file.path().string() + ".hpp");
		if (!hex)
			continue;

		std::string var_name = file.path().filename().string();
		for (char& c : var_name)
		{
			if (c == '.')
				c = '_';
		}

		hex << "#pragma once\n"
			"#include <array>\n"
			"#include <cstdint>\n"
			"\n"
			"inline constexpr std::array<uint8_t, " << bin_size << "> "
			<< var_name
			<< " =\n"
			"{";


		hex << std::hex;
		int i = 0;
		for (int c = bin.get(); c != std::char_traits<char>::eof(); c = bin.get())
		{
			if (i % 20 == 0)
				hex << "\n	";
			hex << "0x" << c;
			if (i != bin_size - 1)
				hex << ", ";
			i++;
		}
		hex << "\n};";

		std::cout << "Converted: " << file.path().string() << '\n';
	}
	return 0;
}