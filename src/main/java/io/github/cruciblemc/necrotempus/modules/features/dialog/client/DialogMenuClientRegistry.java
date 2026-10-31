package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

public interface DialogMenuClientRegistry {

    static void init() {
        DialogMenuClientManager.register();
    }
}
