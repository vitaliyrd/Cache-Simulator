package test;

/*import model_old.Instruction;
import model_old.MainSystem;*/
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Vitaliy on 6/12/15.
 */
public class MainSystemTest {
    /*private MainSystem mainSystem;

    @Test
    public void test() {
        Map<String, Integer> configuration = new HashMap<>();
        configuration.put("l1d_blocks", 32);
        configuration.put("l1d_blockSize", 16);
        configuration.put("l1d_associativity", 1);
        configuration.put("l1d_latency", 1);

        configuration.put("l1i_blocks", 32);
        configuration.put("l1i_blockSize", 16);
        configuration.put("l1i_associativity", 1);
        configuration.put("l1i_latency", 1);

        configuration.put("l2_blocks", 512);
        configuration.put("l2_blockSize", 16);
        configuration.put("l2_associativity", 1);
        configuration.put("l2_latency", 10);

        configuration.put("l3_blocks", 2048);
        configuration.put("l3_blockSize", 16);
        configuration.put("l3_associativity", 1);
        configuration.put("l3_latency", 35);

        configuration.put("1lm_size", 16*1024);
        configuration.put("1lm_readLatency", 100);
        configuration.put("1lm_writeLatency", 100);

        configuration.put("2lm_size", 1024*1024);
        configuration.put("2lm_readLatency", 250);
        configuration.put("2lm_writeLatency", 400);

        mainSystem = new MainSystem(configuration);
        mainSystem.debugOn(true);

        Instruction i1 = new Instruction();
        i1.instruction = 1000 * 1024;
        i1.memoryAction = Instruction.MemoryAction.READ;
        i1.data = 13 * 1024;
        mainSystem.execute(i1);    // Instruction and Data should miss.

        System.out.println();

        mainSystem.execute(i1);    // Instruction and Data should hit.

        System.out.println();

        Instruction i2 = new Instruction();
        i2.instruction = 1023*1024 + 1;
        i2.memoryAction = Instruction.MemoryAction.WRITE;
        i2.data = 1000;
        mainSystem.execute(i2);    // Instruction should miss and Data should be added to Caches without any misses (write operation).

        System.out.println();

        Instruction i3 = new Instruction();
        i3.instruction = 18*1024;
        mainSystem.execute(i3);    // Instruction should miss.
    }*/

}