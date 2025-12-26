package hu.jgj52.catcapes.client;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.ExcludeFromScreen;
import io.wispforest.owo.config.annotation.Modmenu;

import java.util.ArrayList;
import java.util.List;

@Modmenu(modId = "catcapes")
@Config(name = "catcapes", wrapperName = "Config", defaultHook = true)
public class ConfigModel {
    public String cape = "";
    public String token = "";
    @ExcludeFromScreen
    public List<String> animatedCapes = new ArrayList<>();
    public Double update = 0.5;
}
