package beta.manager;

interface IBetaService {
    int getVersion();
    String executeCommand(String command);
    String getPluginStatus(String pluginId);
    boolean installPlugin(String zipPath);
    boolean removePlugin(String pluginId);
    boolean disablePlugin(String pluginId);
    boolean enablePlugin(String pluginId);
    boolean runPluginAction(String pluginId);
    String[] listPlugins();
    boolean isRunning();
    String getServerPath();
    int getApiLevel();
    String getArchitecture();
}
