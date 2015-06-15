import com.opencsv.CSVReader;
import model.Instruction;
import model.SystemBus;

import java.awt.print.Printable;
import java.io.*;
import java.util.*;

/**
 * Entry point for the simulator
 *
 * @author Alex Glass, Vitaliy Radchishin, Andy Tran, Tru Truong
 * @version 1.0
 */
public class Main {
    private static PrintStream output;

    private static SystemBus bus;

    public static void main(String... args) {
        Map<String, Integer> config;
        List<Instruction> instructions;

        try {
            output = new PrintStream(new File("output.txt"));
            config = readConfig(new File("config.csv"));
            instructions = readTrace(new File("trace-2k.csv"));
            bus = new SystemBus(config);
            int execute = 0;
            int cpu = 1;
            for(Instruction i : instructions) {
                if(execute == 50) {
                    if(cpu == 1) cpu = 2;
                    else if(cpu == 2) cpu = 1;
                    execute = 0;
                }
                bus.execute(i, cpu);
                execute++;
            }
            outputStatistics(bus.gatherStatistics(), bus.getStateChanges());

        } catch (IOException e) {
            output.println("Error reading input files.");
        }
    }

    public static List<Instruction> readTrace(File file) throws IOException {
        CSVReader reader = new CSVReader(new FileReader(file));
        List<Instruction> list = new ArrayList<>();

        String line[];
        while((line = reader.readNext()) != null) {
            Instruction instruction = new Instruction();
            instruction.instruction = Long.parseLong(line[0], 16);

            if(line.length == 1) {
                list.add(instruction);
                continue;
            }

            if((line[1]).equals("0")) {
                instruction.memoryAction = Instruction.MemoryAction.READ;
            } else if(line[1].equals("1")) {
                instruction.memoryAction = Instruction.MemoryAction.WRITE;
            }
            if(!line[2].equals("")) {
                instruction.data = Long.parseLong(line[2], 16);
            }
            list.add(instruction);
        }

        reader.close();
        return list;
    }

    public static Map<String, Integer> readConfig(File file) throws IOException {
        CSVReader reader = new CSVReader(new FileReader(file));
        Map<String, Integer> config = new HashMap<>();

        String line[];
        while((line = reader.readNext()) != null) {
            if(line[0].equals("writeScheme")) {
                if(line[1].equals("Write Back")) {
                    config.put(line[0], 0);
                } else {
                    config.put(line[0], 1);
                }
            } else {
                config.put(line[0], Integer.parseInt(line[1]));
            }
        }

        reader.close();
        return config;
    }

    public static void outputStatistics(Map<String, Integer> stats, int stateChanges[][]) {
        for(String s : stats.keySet()) {
            output.println(s + ": " + stats.get(s));
        }

        output.println();
        output.println("Average time per instruction: " + (float)(stats.get("CPU #1 Instruction Count")
                + stats.get("CPU #2 Instruction Count"))/stats.get("Running Time"));
        output.println("Total time: " + stats.get("Running Time"));

        output.println();
        output.println("State changes:");
        output.println("Modified to Exclusive: " + stateChanges[0][1]);
        output.println("Modified to Shared: " + stateChanges[0][2]);
        output.println("Modified to Invalid: " + stateChanges[0][3]);
        output.println("Exclusive to Modified: " + stateChanges[1][0]);
        output.println("Exclusive to Shared: " + stateChanges[1][2]);
        output.println("Exclusive to Invalid: " + stateChanges[1][3]);
        output.println("Shared to Modified: " + stateChanges[2][0]);
        output.println("Shared to Exclusive: " + stateChanges[2][1]);
        output.println("Shared to Invalid: " + stateChanges[2][3]);
        output.println("Invalid to Modified: " + stateChanges[3][0]);
        output.println("Invalid to Exclusive: " + stateChanges[3][1]);
        output.println("Invalid to Shared: " + stateChanges[3][2]);
    }
}
