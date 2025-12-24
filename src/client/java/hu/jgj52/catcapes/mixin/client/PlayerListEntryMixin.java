package hu.jgj52.catcapes.mixin.client;

import hu.jgj52.catcapes.client.Utils;
import net.minecraft.client.network.PlayerListEntry;
//? if <=1.21.8 {
/*import net.minecraft.client.util.SkinTextures;
*///? } else {
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
//? }
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public class PlayerListEntryMixin {

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void modifySkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        SkinTextures original = cir.getReturnValue();

        PlayerListEntry player = (PlayerListEntry) (Object) this;

        Identifier cape = Utils.getCape(Utils.getUUID(player));

        if (cape != null) {
            //? if <=1.21.8 {
            /*cir.setReturnValue(new SkinTextures(
                    original.texture(),
                    original.textureUrl(),
                    cape,
                    cape,
                    original.model(),
                    original.secure()
            ));
            *///? } else {
            cir.setReturnValue(new SkinTextures(
                    original.body(),
                    new AssetInfo.TextureAssetInfo(cape, cape), new AssetInfo.TextureAssetInfo(cape, cape),
                    original.model(), original.secure()
            ));
            //? }
        } else {
            cir.setReturnValue(original);
        }
    }
}