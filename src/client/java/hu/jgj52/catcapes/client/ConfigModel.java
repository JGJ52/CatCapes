package hu.jgj52.catcapes.client;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;

@Modmenu(modId = "catcapes")
@Config(name = "catcapes", wrapperName = "Config", defaultHook = true)
public class ConfigModel {
    public String cape = "";
    public String token = "";
}
