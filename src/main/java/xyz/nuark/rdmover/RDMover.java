package xyz.nuark.rdmover;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("rdmover")
public class RDMover {
    private static final Logger LOGGER = LogManager.getLogger();

    public RDMover() {
        MinecraftForge.EVENT_BUS.register(this);
    }
}
