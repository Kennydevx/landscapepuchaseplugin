package com.seupacote;
//import com.seupacote.TerrenoProtegido.Area;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MinecraftPlugin extends JavaPlugin {

    // Lista de agentes
    private static List<Agent> agents = new ArrayList<>();
    private static final String AGENT_DATA_FILE = "agent_data.json";

    // Configurações do agente
    private double learningRate;
    private double discountFactor;
    private double explorationRate;
    private int numStates;
    private int numActions;

    @Override
    public void onEnable() {
        // Carregar a configuração
        loadConfigSettings();
        getLogger().info("Plugin ativado!");

        // Carregar estado dos agentes do arquivo
        loadAllAgentStates();

        // Criar agentes adicionais se a lista estiver vazia
        if (agents.isEmpty()) {
            createDefaultAgents();
        }

        // Simula uma rotina onde os agentes executam ações periodicamente
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Agent agent : agents) {
                    agent.performAction();
                    getLogger().info("Agente " + agent.getName() + " realizou uma ação com saldo " + agent.getBalance());
                }
            }
        }.runTaskTimer(this, 0L, 100L); // Executa a cada 5 segundos (100 ticks)
    }

    @Override
    public void onDisable() {
        // Salvar o estado dos agentes ao desativar o plugin
        saveAllAgentStates();
        getLogger().info("Plugin desativado!");
    }

    // Carregar configurações do arquivo config.yml
    private void loadConfigSettings() {
        saveDefaultConfig(); // Cria o arquivo config.yml se não existir
        FileConfiguration config = getConfig();

        learningRate = config.getDouble("agent.learningRate", 0.1);
        discountFactor = config.getDouble("agent.discountFactor", 0.9);
        explorationRate = config.getDouble("agent.explorationRate", 0.1);
        numStates = config.getInt("agent.numStates", 5);
        numActions = config.getInt("agent.numActions", 5);
    }

    // Criar agentes padrão
    private void createDefaultAgents() {
        Agent agent1 = new Agent("Agente1", numStates, numActions, learningRate, discountFactor, explorationRate);
        Agent agent2 = new Agent("Agente2", numStates, numActions, learningRate, discountFactor, explorationRate);
        agents.add(agent1);
        agents.add(agent2);
        getLogger().info("Agentes padrão criados.");
    }

    // Salvar todos os estados dos agentes
    private void saveAllAgentStates() {
        try {
            JSONObject json = new JSONObject();
            for (Agent agent : agents) {
                json.put(agent.getName(), serializeAgentState(agent));
            }

            // Escrever os dados em um arquivo
            FileWriter file = new FileWriter(new File(getDataFolder(), AGENT_DATA_FILE));
            file.write(json.toString(4)); // Formatação com recuo para facilitar a leitura
            file.flush();
            file.close();
            getLogger().info("Estado dos agentes salvo com sucesso.");
        } catch (IOException e) {
            getLogger().severe("Erro ao salvar o estado dos agentes: " + e.getMessage());
        }
    }

    // Método auxiliar para serializar o estado do agente
    private JSONObject serializeAgentState(Agent agent) {
        JSONObject agentData = new JSONObject();
        agentData.put("name", agent.getName());
        agentData.put("balance", agent.getBalance());

        // Serializar inventário do agente
        JSONArray inventoryArray = new JSONArray();
        for (ItemStack item : agent.getInventory().getContents()) {
            if (item != null) {
                inventoryArray.put(item.serialize());
            }
        }
        agentData.put("inventory", inventoryArray);

        return agentData;
    }

    // Carregar todos os estados dos agentes
    private void loadAllAgentStates() {
        try {
            File file = new File(getDataFolder(), AGENT_DATA_FILE);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(file.getPath())));
                JSONObject json = new JSONObject(content);

                for (String key : json.keySet()) {
                    JSONObject agentData = json.getJSONObject(key);
                    String name = agentData.getString("name");
                    double balance = agentData.getDouble("balance");

                    Agent agent = new Agent(name, numStates, numActions, learningRate, discountFactor, explorationRate);
                    agent.setBalance(balance);

                    // Carregar inventário
                    Inventory inventory = Bukkit.createInventory(null, 36); // Exemplo de inventário padrão
                    JSONArray inventoryArray = agentData.getJSONArray("inventory");
                    for (int i = 0; i < inventoryArray.length(); i++) {
                        inventory.setItem(i, ItemStack.deserialize(inventoryArray.getJSONObject(i).toMap()));
                    }
                    agent.setInventory(inventory);

                    agents.add(agent);
                }
                getLogger().info("Todos os estados dos agentes foram carregados.");
            }
        } catch (IOException e) {
            getLogger().severe("Erro ao carregar o estado dos agentes: " + e.getMessage());
        }
    }

    // Comando para spawnar o agente como um player
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("spawnAgent") && args.length > 0) {
            String agentName = args[0];
            Agent agent = findAgentByName(agentName);
            if (agent != null) {
                // Simulação de um agente como um player
                Player fakePlayer = Bukkit.getPlayer(agentName);
                if (fakePlayer == null) {
                    getLogger().info("O agente " + agentName + " foi spawnado como um jogador simulado.");
                    // Aqui você pode implementar a lógica para exibir o agente no mundo
                } else {
                    sender.sendMessage("O agente " + agentName + " já está no jogo.");
                }
                return true;
            } else {
                sender.sendMessage("Agente " + agentName + " não encontrado.");
                return false;
            }
        }
        return false;
    }

    // Método para encontrar um agente pelo nome
    private Agent findAgentByName(String agentName) {
        for (Agent agent : agents) {
            if (agent.getName().equalsIgnoreCase(agentName)) {
                return agent;
            }
        }
        return null;
    }
}
