plugins {
	id("mod-platform")
	id("net.neoforged.moddev")
}

platform {
	loader = "neoforge"
	dependencies {
		required("minecraft") {
			forgeVersionRange = "[${prop("mc_version_range")}]"
		}
		required("neoforge") {
			forgeVersionRange = "[1,)"
		}
		required("factory_api") {
			slug("factory-api")
			versionRange = ">=${prop("factory_api_version")}"
		}
	}
}

neoForge {
	version = prop("neoforge_version")
	accessTransformers.from(project.file(platform.atFile))
	validateAccessTransformers = true

	if (hasProperty("deps.parchment")) parchment {
		val (mc, ver) = (property("deps.parchment") as String).split(':')
		mappingsVersion = ver
		minecraftVersion = mc
	}

	runs {
		register("client") {
			client()
			gameDirectory = file("run/")
			ideName = "NeoForge Client (${stonecutter.active?.version})"
			programArgument("--username=Dev")
		}
		register("server") {
			server()
			gameDirectory = file("run/")
			ideName = "NeoForge Server (${stonecutter.active?.version})"
		}
	}

	mods {
		register(property("mod_id") as String) {
			sourceSet(sourceSets["main"])
		}
	}
	sourceSets["main"].resources.srcDir("${rootDir}/versions/datagen/${stonecutter.current.version.split("-")[0]}/src/main/generated")
}

repositories {
	mavenCentral()
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
	maven("https://raw.githubusercontent.com/Kyubion-Studios/Mod-Resources/main/maven/") { name = "Kyubion Mod Resources" }
	maven("https://maven.isxander.dev/releases")
}

dependencies {
	implementation(libs.moulberry.mixinconstraints)
	jarJar(libs.moulberry.mixinconstraints)

	api("wily.factory_api:factory_api-neoforge:${stonecutter.current.version}-${prop("factory_api_version")}")
	compileOnly(rootProject.fileTree("leg four jay"))
	compileOnly("wily.legacy:legacy-neoforge:${stonecutter.current.version}-${prop("legacy4j_version")}") { isTransitive = false }
}

tasks.withType<Javadoc> {
	enabled = false
}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}
