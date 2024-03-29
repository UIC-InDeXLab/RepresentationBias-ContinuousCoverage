package cli;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Cli {
	private static final Logger log = Logger.getLogger(Cli.class.getName());
	private String[] args;
	private Options options;
	private CommandLine cmd;

	public final static String ARG_HELP = "h";
	public final static String ARG_INPUT = "i";
	public final static String ARG_SCHEMA = "s";
	public final static String ARG_K = "k";
	public final static String ARG_RHO = "r";
	public final static String ARG_NUM_QUERIES = "n";
	public final static String ARG_REPEAT = "p";
	public final static String ARG_ATTRS = "a";
	public final static String ARG_OUTPUT = "o";
	public final static String ARG_EPSILON = "e";
	public final static String ARG_PHI = "phi";

	private final static String MSG_HELP = "show help";
	private final static String MSG_INPUT = "input dataset data file name";
	private final static String MSG_SCHEMA = "input dataset schema file name";
	private final static String MSG_K = "k values";
	private final static String MSG_RHO = "rho values";
	private final static String MSG_ATTRS = "selected attribute values";
	private final static String MSG_NUM_QUERIES = "number of query points";
	private final static String MSG_NUM_REPEATS = "number of repeats";
	private final static String MSG_OUTPUT = "if store test result in a file";
	private final static String MSG_EPSILON = "epsilon values";
	private final static String MSG_PHI = "phi values";

	private final static String[] MANDATORY_OPTIONS = new String[]{ARG_INPUT,
			ARG_SCHEMA, ARG_K, ARG_RHO, ARG_NUM_QUERIES, ARG_REPEAT};

	public Cli(String[] args) {
		this.args = args;

		// Define command line interface
		options = new Options();

		options.addOption(ARG_HELP, false, MSG_HELP);
		options.addOption(ARG_OUTPUT, false, MSG_OUTPUT);

		options.addOption(ARG_INPUT, true, MSG_INPUT); // input file arg
		options.addOption(ARG_SCHEMA, true, MSG_SCHEMA); // input file arg

		// k values arg
		Option kOpt = new Option(ARG_K, MSG_K);
		kOpt.setArgs(Option.UNLIMITED_VALUES);
		kOpt.setValueSeparator(',');
		options.addOption(kOpt);

		// rho values arg
		Option rhoOpt = new Option(ARG_RHO, true, MSG_RHO);
		rhoOpt.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(rhoOpt);
		// numQueries values arg
		Option numQueryPtsOpt = new Option(ARG_NUM_QUERIES, true, MSG_NUM_QUERIES);
		numQueryPtsOpt.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(numQueryPtsOpt);
		// attribute values
		Option attrOpt = new Option(ARG_ATTRS, true, MSG_ATTRS);
		attrOpt.setArgs(Option.UNLIMITED_VALUES);
		
		options.addOption(attrOpt);
		// repeat values arg
		Option repeatOpt = new Option(ARG_REPEAT, true, MSG_NUM_REPEATS);
		options.addOption(repeatOpt);

		// epsilon values arg
		Option epsilonOpt = new Option(ARG_EPSILON, true, MSG_EPSILON);
		epsilonOpt.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(epsilonOpt);

		// epsilon values arg
		Option phiOpt = new Option(ARG_PHI, true, MSG_PHI);
		phiOpt.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(phiOpt);

		parse();
	}

	/**
	 * Parse arguments
	 */
	private void parse() {
		CommandLineParser parser = new DefaultParser();
		try {
			cmd = parser.parse(options, args);
			if (cmd.hasOption(ARG_HELP))
				help();
			for (String manOp : MANDATORY_OPTIONS) {
				if (!cmd.hasOption(manOp)) {
					System.err.println("[ERROR] Missing option = " + manOp);
					help();
				}
			}
			
			if (cmd.hasOption(ARG_EPSILON) && !cmd.hasOption(ARG_PHI)) {
				System.err.println("[ERROR] Missing option = " + ARG_PHI);
				help();
			}
			
			if (!cmd.hasOption(ARG_EPSILON) && cmd.hasOption(ARG_PHI)) {
				System.err.println("[ERROR] Missing option = " + ARG_EPSILON);
				help();
			}

		} catch (ParseException e) {
			log.log(Level.SEVERE, "Failed to parse comand line properties", e);
			help();
			System.exit(0);
		}
	}

	/**
	 * Get argument value
	 * 
	 * @param argName
	 * @return
	 */
	public String getArgValue(String argName) {
		if (cmd.hasOption(argName))
			return cmd.getOptionValue(argName);
		try {
			throw new Exception(String.format(
					"getArgValue ERROR: Argument [%s] not found. Exit.",
					argName));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		help();
		System.exit(0);
		return null;
	}

	/**
	 * Get argument values (more than 1)
	 * 
	 * @param argName
	 * @return
	 */
	public String[] getArgValues(String argName) {
		if (cmd.hasOption(argName))
			return cmd.getOptionValues(argName);

		try {
			throw new Exception(String.format(
					"getArgValues ERROR: Argument [%s] not found. Exit.",
					argName));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		help();
		System.exit(0);
		return null;
	}

	/**
	 * Check if argument exists
	 * 
	 * @param argName
	 * @return
	 */
	public boolean hasOption(String argName) {
		return cmd.hasOption(argName);
	}

	/**
	 * Print help info
	 */
	private void help() {
		// This prints out some help
		HelpFormatter formater = new HelpFormatter();

		formater.printHelp("Main", options);
		System.exit(0);
	}

	@Override
	public String toString() {
		String output = "";
		for (Option o : cmd.getOptions()) {
			output += o.getOpt() + "=" + Arrays.toString(o.getValues()) + "\n";
		}

		return output;
	}
}
