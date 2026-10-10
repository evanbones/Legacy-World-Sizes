import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
	id("mod-platform")
	id("dev.kikugie.loom-back-compat")
}

platform {
	loader = "fabric"
	dependencies {
		required("minecraft") {
			versionRange = stonecutter.current.version
		}
		required("factory_api") {
			slug("factory-api")
			versionRange = ">=${prop("factory_api_version")}"
		}
		required("fabricloader") {
			versionRange = ">=${libs.fabric.loader.get().version}"
		}
		optional("modmenu") {}
	}
}

configurations.configureEach {
	resolutionStrategy {
		force("net.fabricmc:fabric-loader:${prop("fabric_loader_version")}")
	}
}

val isUnobfuscated = loomx.isUnobfuscated

fun String.toNamedAccessWidener() = replaceFirst(Regex("^(accessWidener\\s+v\\d+\\s+)official"), "$1named")

val accessWidener = if (isUnobfuscated) rootProject.file(platform.awFile) else layout.buildDirectory.file("aw/${rootProject.file(platform.awFile).name}").get().asFile.apply {
	parentFile.mkdirs()
	writeText(rootProject.file(platform.awFile).readText().toNamedAccessWidener())
}

if (!isUnobfuscated) tasks.named<ProcessResources>("processResources") {
	filesMatching("aw/*.accesswidener") { filter { it.toNamedAccessWidener() } }
}

extensions.configure<LoomGradleExtensionAPI>("loom") {
	accessWidenerPath = accessWidener
	runs.named("client") {
		client()
		ideConfigGenerated(true)
		runDir = "run/"
		environment = "client"
		programArgs("--username=Dev")
		configName = "Fabric Client"
	}
	runs.named("server") {
		server()
		ideConfigGenerated(true)
		runDir = "run/"
		environment = "server"
		configName = "Fabric Server"
	}
	if (!isUnobfuscated) mixin {
		useLegacyMixinAp = true
		defaultRefmapName = "${prop("mod_id")}.refmap.json"
	}
}

repositories {
	mavenCentral()
	strictMaven("https://maven.terraformersmc.com/", "com.terraformersmc") { name = "TerraformersMC" }
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
	maven("https://raw.githubusercontent.com/Kyubion-Studios/Mod-Resources/main/maven/") { name = "Kyubion Mod Resources" }
	maven("https://maven.isxander.dev/releases")
}

dependencies {
	minecraft("com.mojang:minecraft:${stonecutter.current.version}")
	loomx.applyMojangMappings()
	modImplementation(libs.fabric.loader)
	modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_api_version")}")
	modImplementation("com.terraformersmc:modmenu:${prop("modmenu_version")}")
	modApi("wily.factory_api:factory_api-fabric:${stonecutter.current.version}-${prop("factory_api_version")}")
	modImplementation("wily.legacy:legacy-fabric:${stonecutter.current.version}-${prop("legacy4j_version")}") { isTransitive = isUnobfuscated }

	implementation(libs.moulberry.mixinconstraints)
	include(libs.moulberry.mixinconstraints)
}

tasks.withType<Javadoc> {
	enabled = false
}
