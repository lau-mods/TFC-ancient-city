package net.claustra01.tfcancientcity.mixin;

import net.claustra01.tfcancientcity.accessor.StructureTemplateIdAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureTemplateManager.class)
public abstract class StructureTemplateManagerMixin {
    @Inject(method = "getOrCreate", at = @At("RETURN"))
    private void tfcAncientCity$rememberTemplateId(
        ResourceLocation templateId,
        CallbackInfoReturnable<StructureTemplate> callback
    ) {
        ((StructureTemplateIdAccessor)(Object)callback.getReturnValue()).tfcAncientCity$setTemplateId(templateId);
    }
}
