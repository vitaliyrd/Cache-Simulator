package test;

/*import model_old.CPU;
import model_old.Instruction;
import model_old.MainSystem;
import org.junit.Test;*/

import java.util.HashMap;
import java.util.Map;

/**
 * Test class for the CPU class.
 *
 * @author Alex Glass, Vitaliy Radchishin, Andy Tran, Tru Truong
 * @version 1.0
 */
public class CPUTest {
    /*private CPU cpu;

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

        cpu = new CPU(configuration, new MainSystem(configuration));
        cpu.debug = true;

        Instruction i1 = new Instruction();
        i1.instruction = 0b01111101011101110001101100111000;
        i1.memoryAction = Instruction.MemoryAction.READ;
        i1.data = 0b01111101011101110001111111111000;
        cpu.execute(i1);    // Instruction and Data should miss.

        System.out.println();

        cpu.execute(i1);    // Instruction and Data should hit.

        System.out.println();

        Instruction i2 = new Instruction();
        i2.instruction = 0b1011010101001101010111101011010;
        i2.memoryAction = Instruction.MemoryAction.WRITE;
        i2.data = 0b10000111110110110101001101011111;
        cpu.execute(i2);    // Instruction should miss and Data should be added to Caches without any misses (write operation).

        System.out.println();

        Instruction i3 = new Instruction();
        i3.instruction = 0b1110101101111010110010100100101;
        cpu.execute(i3);    // Instruction should miss.
    }*/
}