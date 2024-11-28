import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.milkbowl.vault.economy.Economy;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class LandPurchasePlugin extends JavaPlugin {
    private Economy economy;
    private LuckPerms luckPerms;  // Dependência do LuckPerms
    private WorldGuardPlugin worldGuard;

    @Override
    public void onEnable() {
        // Verificação de Vault para Economia
        if (!setupEconomy()) {
            getLogger().severe("Vault não encontrado! Desativando plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // **Modificação**: Verificação do LuckPerms
        if (!setupLuckPerms()) {
            getLogger().severe("LuckPerms não encontrado! Desativando plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Verificação do WorldGuard para proteção de regiões
        worldGuard = getWorldGuard();
        if (worldGuard == null) {
            getLogger().severe("WorldGuard não encontrado! Desativando plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("Plugin de compra de terrenos habilitado!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin de compra de terrenos desabilitado!");
    }

    // Configuração do Vault para Economia
    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    // **Modificação**: Função para configurar LuckPerms
    private boolean setupLuckPerms() {
        try {
            luckPerms = LuckPermsProvider.get();
            return luckPerms != null;
        } catch (Exception e) {
            getLogger().severe("LuckPerms não encontrado ou não carregado!");
            return false;
        }
    }

    // Verificação do WorldGuard
    private WorldGuardPlugin getWorldGuard() {
        return (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Apenas jogadores podem usar esse comando.");
            return true;
        }

        Player player = (Player) sender;
        if (!command.getName().equalsIgnoreCase("comprarterreno")) return false;

        // Verificação de permissão com LuckPerms
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null || !user.getCachedData().getPermissionData().checkPermission("landpurchase.buy").asBoolean()) {
            player.sendMessage("Você não tem permissão para comprar terrenos.");
            return true;
        }

        // Verificação de economia
        if (!economy.has(player, 1000)) {
            player.sendMessage("Você não tem dinheiro suficiente para comprar este terreno.");
            return true;
        }

        economy.withdrawPlayer(player, 1000);
        comprarTerreno(player, 10, 10);
        protegerTerreno(player, 10, 10);
        player.sendMessage("Terreno comprado e protegido com sucesso!");
        return true;
    }

    // Método para compra de terreno
    private void comprarTerreno(Player player, int width, int length) {
        Location loc = player.getLocation();
        int startX = loc.getBlockX() - width / 2;
        int startZ = loc.getBlockZ() - length / 2;
        int y = loc.getBlockY();

        for (int x = startX; x < startX + width; x++) {
            for (int z = startZ; z < startZ + length; z++) {
                Block block = loc.getWorld().getBlockAt(x, y, z);
                block.setType(Material.GRASS_BLOCK);
            }
        }

        for (int x = startX; x <= startX + width; x++) {
            loc.getWorld().getBlockAt(x, y + 1, startZ).setType(Material.OAK_FENCE);
            loc.getWorld().getBlockAt(x, y + 1, startZ + length).setType(Material.OAK_FENCE);
        }
        for (int z = startZ; z <= startZ + length; z++) {
            loc.getWorld().getBlockAt(startX, y + 1, z).setType(Material.OAK_FENCE);
            loc.getWorld().getBlockAt(startX + width, y + 1, z).setType(Material.OAK_FENCE);
        }

        getLogger().info("Terreno comprado e cercado para o jogador " + player.getName());
    }

    // Método para proteção de terreno
    private void protegerTerreno(Player player, int width, int length) {
        if (worldGuard == null) {
            player.sendMessage("Erro ao proteger o terreno. Contate um administrador.");
            return;
        }

        Location loc = player.getLocation();
        int startX = loc.getBlockX() - width / 2;
        int startZ = loc.getBlockZ() - length / 2;
        int endX = startX + width;
        int endZ = startZ + length;

        String regionName = "terreno_" + player.getUniqueId();
        BlockVector3 min = BlockVector3.at(startX, 0, startZ);
        BlockVector3 max = BlockVector3.at(endX, 255, endZ);

        RegionContainer container = worldGuard.getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));

        if (regions == null) {
            player.sendMessage("Erro ao proteger o terreno. Contate um administrador.");
            return;
        }

        if (regions.hasRegion(regionName)) {
            player.sendMessage("Você já possui um terreno. Remova o terreno existente antes de comprar outro.");
            return;
        }

        ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionName, min, max);
        region.getOwners().addPlayer(player.getUniqueId());
        region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY);
        region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);
        region.setFlag(Flags.USE, StateFlag.State.ALLOW);

        regions.addRegion(region);
        player.sendMessage("Proteção de terreno ativada!");
    }
}
