package com.lordmau5.ffs.holder;

import com.lordmau5.ffs.client.ValveRenderer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class FFSBlockRendererManager {
    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FFSBlockEntities.tileEntityFluidValve.get(), ValveRenderer::new);
    }
}
