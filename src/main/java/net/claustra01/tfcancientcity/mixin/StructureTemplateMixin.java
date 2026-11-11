package net.claustra01.tfcancientcity.mixin;

import net.claustra01.tfcancientcity.accessor.StructureTemplateIdAccessor;
import net.claustra01.tfcancientcity.world.TfcBlockReplacementProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin implements StructureTemplateIdAccessor {
    @Unique
    private ResourceLocation tfcAncientCity$templateId;

    @Override
    public ResourceLocation tfcAncientCity$getTemplateId() {
        return tfcAncientCity$templateId;
    }

    @Override
    public void tfcAncientCity$setTemplateId(ResourceLocation templateId) {
        tfcAncientCity$templateId = templateId;
    }

    @Inject(method = "placeInWorld", at = @At("HEAD"))
    private void tfcAncientCity$addReplacementProcessor(
        ServerLevelAccessor level,
        BlockPos offset,
        BlockPos pos,
        StructurePlaceSettings settings,
        RandomSource random,
        int flags,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (tfcAncientCity$templateId != null
            && tfcAncientCity$templateId.getNamespace().equals("minecraft")
            && tfcAncientCity$templateId.getPath().startsWith("ancient_city/")
            && !settings.getProcessors().contains(TfcBlockReplacementProcessor.INSTANCE)) {
            settings.addProcessor(TfcBlockReplacementProcessor.INSTANCE);
        }
    }
}
