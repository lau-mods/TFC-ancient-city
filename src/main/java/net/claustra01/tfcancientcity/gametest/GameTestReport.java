package net.claustra01.tfcancientcity.gametest;

import java.io.File;
import javax.xml.parsers.ParserConfigurationException;
import net.claustra01.tfcancientcity.TfcAncientCity;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;

@EventBusSubscriber(modid = TfcAncientCity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class GameTestReport {
    @SubscribeEvent
    public static void configure(RegisterGameTestsEvent event) {
        if (!GameTestHooks.isGametestServer()) {
            return;
        }
        String reportPath = System.getProperty("tfcancientcity.gametestReport");
        if (reportPath == null) {
            return;
        }
        File report = new File(reportPath);
        report.getParentFile().mkdirs();
        try {
            GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(report));
        } catch (ParserConfigurationException exception) {
            throw new IllegalStateException("Could not create the GameTest report", exception);
        }
    }

    private GameTestReport() {
    }
}
