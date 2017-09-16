package net.brutus5000.bireus;

import net.brutus5000.bireus.patching.PatchTask;
import net.brutus5000.bireus.patching.PatchTaskFactory;
import net.brutus5000.bireus.patching.PatchTaskV1;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PatchTaskFactoryTest {
    private PatchTaskFactory instance;

    @Before
    public void setUp() throws Exception {
        instance = PatchTaskFactory.getInstance();
    }

    @Test
    public void testCreateV1() throws Exception {
        PatchTask task = instance.create(1);

        assertEquals(task.getClass(), PatchTaskV1.class);
        assertEquals(task.getVersion(), 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNonExistant() throws Exception {
        instance.create(999);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddWrongClass() throws Exception {
        instance.add(999, String.class);
    }
}
