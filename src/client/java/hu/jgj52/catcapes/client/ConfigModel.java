package hu.jgj52.catcapes.client;

import io.wispforest.owo.config.annotation.*;
import io.wispforest.owo.config.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@Modmenu(modId = "catcapes")
@Config(name = "catcapes", wrapperName = "Config")
public class ConfigModel {
    @Hook
    public Boolean online = true;

    @Hook
    public String cape = "";

    public String token = "";

    public Double update = 0.5;

    @ExcludeFromScreen
    public List<String> animatedCapes = new ArrayList<>();
}
