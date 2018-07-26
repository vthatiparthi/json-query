package io.json;

import com.beust.jcommander.Parameter;

public class AppParameters {

	@Parameter(names = { "--help" }, help = true, description = "Display help and exit")
	public boolean help;
	
	@Parameter(names = { "-input-file" }, description = "Input json file name with absolute path")
	public String inputFile;
	
	@Parameter(names = { "-limit-size" }, description = "Limit Size")
	public int limitSize;
	
	@Parameter(names = { "-output-file" }, description = "Output csv file name with absolute path")
	public String outputFile;
}
