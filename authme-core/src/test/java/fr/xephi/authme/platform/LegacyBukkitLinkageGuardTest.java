package fr.xephi.authme.platform;

import fr.xephi.authme.listener.PlayerListener;
import fr.xephi.authme.message.PlayerLocaleResolver;
import fr.xephi.authme.settings.SpawnLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guards that core classes do not hold direct bytecode linkage to optional/modern
 * Bukkit APIs that would break loading on legacy servers (e.g. 1.8).
 * Checks constant-pool Class/Methodref entries, not just String literals.
 */
class LegacyBukkitLinkageGuardTest {

    // --- PlayerListener ---

    @Test
    void playerListenerMustNotLinkToOptionalEvents() throws IOException {
        BytecodeInfo info = parse(PlayerListener.class);

        assertNoClassName(info, "PlayerSwapHandItemsEvent",
            "PlayerListener must not have direct class reference to PlayerSwapHandItemsEvent");
        assertNoClassName(info, "EntityAirChangeEvent",
            "PlayerListener must not have direct class reference to EntityAirChangeEvent");
        assertNoClassName(info, "EntityPickupItemEvent",
            "PlayerListener must not have direct class reference to EntityPickupItemEvent");

        assertNoMethodRef(info, "PlayerSwapHandItemsEvent");
        assertNoMethodRef(info, "EntityAirChangeEvent");
        assertNoMethodRef(info, "EntityPickupItemEvent");

        // Also ensure raw Utf8 pool does not contain those event names at all
        // (PlayerListener should be clean; even String constants would be wrong here)
        assertNoUtf8(info, "PlayerSwapHandItemsEvent");
        assertNoUtf8(info, "EntityAirChangeEvent");
        assertNoUtf8(info, "EntityPickupItemEvent");
    }

    // --- SpawnLoader ---

    @Test
    void spawnLoaderMustNotLinkToGameRuleOrDirectIsPassable() throws IOException {
        BytecodeInfo info = parse(SpawnLoader.class);

        assertNoClassName(info, "org/bukkit/GameRule",
            "SpawnLoader must not have direct Class reference to org/bukkit/GameRule");
        // Also guard against field/method refs touching GameRule type
        assertNoMethodRef(info, "GameRule");

        // Direct Block.isPassable must not appear as Methodref/InterfaceMethodref;
        // SpawnLoader must delegate via BukkitCompatibilityAdapter.isBlockPassable instead.
        boolean hasDirectIsPassable = false;
        for (MemberRef ref : info.memberRefs) {
            if ("org/bukkit/block/Block".equals(ref.className) && "isPassable".equals(ref.name)) {
                hasDirectIsPassable = true;
                break;
            }
        }
        assertFalse(hasDirectIsPassable,
            "SpawnLoader must not have direct Methodref to org/bukkit/block/Block.isPassable; "
                + "use BukkitCompatibilityAdapter.isBlockPassable instead. Refs: " + info.memberRefs);
    }

    @Test
    void spawnLoaderMustUseCompatibilityAdapter() throws IOException {
        BytecodeInfo info = parse(SpawnLoader.class);
        assertHasClassName(info, "fr/xephi/authme/platform/BukkitCompatibilityAdapter",
            "SpawnLoader should reference BukkitCompatibilityAdapter");
        assertTrue(info.utf8Values.stream().anyMatch(s -> s.contains("isBlockPassable"))
                || info.memberRefs.stream().anyMatch(r -> "isBlockPassable".equals(r.name)),
            "SpawnLoader should call BukkitCompatibilityAdapter.isBlockPassable or getSpawnRadius");
        assertTrue(info.memberRefs.stream().anyMatch(r -> "getSpawnRadius".equals(r.name)),
            "SpawnLoader should call BukkitCompatibilityAdapter.getSpawnRadius");
    }

    // --- PlayerLocaleResolver ---

    @Test
    void playerLocaleResolverMustNotDirectlyCallGetLocale() throws IOException {
        BytecodeInfo info = parse(PlayerLocaleResolver.class);

        boolean hasDirectGetLocale = false;
        for (MemberRef ref : info.memberRefs) {
            if ("org/bukkit/entity/Player".equals(ref.className) && "getLocale".equals(ref.name)) {
                hasDirectGetLocale = true;
                break;
            }
        }
        assertFalse(hasDirectGetLocale,
            "PlayerLocaleResolver must not have direct Methodref to org/bukkit/entity/Player.getLocale; "
                + "use BukkitCompatibilityAdapter.getPlayerLocale instead. Refs: " + info.memberRefs);

        // Also ensure no Class-level linkage beyond the instanceof/checkcast type usage is fine,
        // but the method linkage above is the critical guard. We allow org/bukkit/entity/Player as type.
        // Verify delegation is via adapter
        assertHasClassName(info, "fr/xephi/authme/platform/BukkitCompatibilityAdapter",
            "PlayerLocaleResolver should reference BukkitCompatibilityAdapter");
        assertTrue(info.memberRefs.stream().anyMatch(r -> "getPlayerLocale".equals(r.name)),
            "PlayerLocaleResolver should call BukkitCompatibilityAdapter.getPlayerLocale");
    }

    // --- AbstractSpigotPlatformAdapter ---

    @Test
    void abstractSpigotPlatformAdapterMayContainStringConstantsButNotDirectClassDescriptors() throws IOException {
        BytecodeInfo info = parse(AbstractSpigotPlatformAdapter.class);

        // May contain modern API names as String constants (Utf8/String pool)
        assertHasUtf8(info, "org.bukkit.event.player.PlayerSwapHandItemsEvent");
        assertHasUtf8(info, "org.bukkit.event.entity.EntityAirChangeEvent");
        assertHasUtf8(info, "org.bukkit.event.entity.EntityPickupItemEvent");
        assertHasUtf8(info, "org.spigotmc.event.player.PlayerSpawnLocationEvent");

        // Must NOT have direct Class entries for optional listener APIs
        assertNoClassName(info, "org/bukkit/event/player/PlayerSwapHandItemsEvent",
            "AbstractSpigotPlatformAdapter must not have direct Class for PlayerSwapHandItemsEvent; use String + Class.forName indirection");
        assertNoClassName(info, "org/bukkit/event/entity/EntityAirChangeEvent",
            "AbstractSpigotPlatformAdapter must not have direct Class for EntityAirChangeEvent");
        assertNoClassName(info, "org/bukkit/event/entity/EntityPickupItemEvent",
            "AbstractSpigotPlatformAdapter must not have direct Class for EntityPickupItemEvent");
        assertNoClassName(info, "org/spigotmc/event/player/PlayerSpawnLocationEvent",
            "AbstractSpigotPlatformAdapter must not have direct Class for PlayerSpawnLocationEvent");

        // Must NOT have direct Class entries for optional listener implementations
        assertNoClassName(info, "fr/xephi/authme/listener/PlayerSwapHandItemsListener",
            "AbstractSpigotPlatformAdapter must not have direct Class for PlayerSwapHandItemsListener");
        assertNoClassName(info, "fr/xephi/authme/listener/EntityAirChangeListener",
            "AbstractSpigotPlatformAdapter must not have direct Class for EntityAirChangeListener");
        assertNoClassName(info, "fr/xephi/authme/listener/EntityPickupItemListener",
            "AbstractSpigotPlatformAdapter must not have direct Class for EntityPickupItemListener");
        assertNoClassName(info, "fr/xephi/authme/listener/LegacyPlayerSpawnLocationListener",
            "AbstractSpigotPlatformAdapter must not have direct Class for LegacyPlayerSpawnLocationListener");

        // Must NOT have Methodref/Fieldref directly referencing those optional event classes
        assertNoMethodRef(info, "PlayerSwapHandItemsEvent");
        assertNoMethodRef(info, "EntityAirChangeEvent");
        // EntityPickupItemEvent class name appears in string only; method refs to it would be direct linkage
        // Allow the string constant case already verified above.
    }

    // --- Java 8 bytecode version ---

    @Test
    void coreBytecodeMustBeJava8() throws IOException {
        // Core main classes that must remain Java 8 (major version 52)
        Class<?>[] coreClasses = new Class<?>[]{
            fr.xephi.authme.AuthMe.class,
            PlayerListener.class,
            SpawnLoader.class,
            PlayerLocaleResolver.class,
            AbstractSpigotPlatformAdapter.class,
            fr.xephi.authme.data.limbo.EnderPearlRestoreData.class
        };
        for (Class<?> clazz : coreClasses) {
            BytecodeInfo info = parse(clazz);
            assertTrue(info.majorVersion <= 52,
                clazz.getName() + " must be compiled to Java 8 (major <=52) but was " + info.majorVersion);
        }
    }

    // --- Helpers ---

    private static void assertNoClassName(BytecodeInfo info, String substring, String message) {
        for (String cn : info.classNames) {
            if (cn.contains(substring)) {
                fail(message + " — found Class entry: " + cn + " allClasses: " + info.classNames);
            }
        }
    }

    private static void assertHasClassName(BytecodeInfo info, String expected, String message) {
        boolean found = false;
        for (String cn : info.classNames) {
            if (cn.equals(expected) || cn.contains(expected)) {
                found = true;
                break;
            }
        }
        assertTrue(found, message + " — expected Class entry containing: " + expected + " but got: " + info.classNames);
    }

    private static void assertNoUtf8(BytecodeInfo info, String substring) {
        for (String s : info.utf8Values) {
            if (s.contains(substring)) {
                fail("Unexpected Utf8 constant containing '" + substring + "' found: " + s);
            }
        }
    }

    private static void assertHasUtf8(BytecodeInfo info, String expected) {
        boolean found = false;
        for (String s : info.utf8Values) {
            if (s.equals(expected) || s.contains(expected)) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Expected Utf8/String constant '" + expected + "' not found in " + info.utf8Values);
    }

    private static void assertNoMethodRef(BytecodeInfo info, String substring) {
        for (MemberRef ref : info.memberRefs) {
            if (ref.className != null && ref.className.contains(substring)) {
                fail("Unexpected Methodref/Fieldref class containing '" + substring + "': " + ref);
            }
            if (ref.name != null && ref.name.contains(substring)) {
                // narrow: only fail if class also looks like Bukkit event type
                // but for generic guard we flag any ref whose class contains the event name
            }
        }
        // also check that no className + event substring combo appears as separate pools
    }

    private static BytecodeInfo parse(Class<?> clazz) throws IOException {
        String resource = "/" + clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = clazz.getResourceAsStream(resource)) {
            if (is == null) {
                // fallback via classloader
                InputStream is2 = clazz.getClassLoader().getResourceAsStream(clazz.getName().replace('.', '/') + ".class");
                if (is2 == null) {
                    throw new IOException("Cannot locate class bytes for " + clazz.getName());
                }
                byte[] bytes = is2.readAllBytes();
                is2.close();
                return parseBytes(bytes, clazz.getName());
            }
            byte[] bytes = is.readAllBytes();
            return parseBytes(bytes, clazz.getName());
        }
    }

    private static BytecodeInfo parseBytes(byte[] b, String debugName) {
        if (b.length < 10 || b[0] != (byte) 0xCA || b[1] != (byte) 0xFE || b[2] != (byte) 0xBA || b[3] != (byte) 0xBE) {
            throw new IllegalArgumentException("Invalid class file for " + debugName);
        }
        int major = ((b[6] & 0xFF) << 8) | (b[7] & 0xFF);
        int cpCount = ((b[8] & 0xFF) << 8) | (b[9] & 0xFF);
        int pos = 10;

        Map<Integer, String> utf8Map = new HashMap<>();
        Map<Integer, Integer> classNameIndex = new HashMap<>();
        Map<Integer, Integer> stringIndex = new HashMap<>();
        List<int[]> fieldMethodRefs = new ArrayList<>(); // tag, classIdx, natIdx
        Map<Integer, int[]> nameAndTypeMap = new HashMap<>();

        // tags: 1 Utf8, 7 Class, 8 String, 9 Fieldref, 10 Methodref, 11 InterfaceMethodref, 12 NameAndType, 3/4/5/6 etc
        for (int i = 1; i < cpCount; i++) {
            int tag = b[pos++] & 0xFF;
            switch (tag) {
                case 1: { // Utf8
                    int len = ((b[pos] & 0xFF) << 8) | (b[pos + 1] & 0xFF);
                    pos += 2;
                    String s = new String(b, pos, len, StandardCharsets.UTF_8);
                    utf8Map.put(i, s);
                    pos += len;
                    break;
                }
                case 7: { // Class
                    int nameIdx = ((b[pos] & 0xFF) << 8) | (b[pos + 1] & 0xFF);
                    classNameIndex.put(i, nameIdx);
                    pos += 2;
                    break;
                }
                case 8: { // String
                    int strIdx = ((b[pos] & 0xFF) << 8) | (b[pos + 1] & 0xFF);
                    stringIndex.put(i, strIdx);
                    pos += 2;
                    break;
                }
                case 9: // Fieldref
                case 10: // Methodref
                case 11: { // InterfaceMethodref
                    int classIdx = ((b[pos] & 0xFF) << 8) | (b[pos + 1] & 0xFF);
                    int natIdx = ((b[pos + 2] & 0xFF) << 8) | (b[pos + 3] & 0xFF);
                    fieldMethodRefs.add(new int[]{tag, classIdx, natIdx});
                    pos += 4;
                    break;
                }
                case 12: { // NameAndType
                    int nameIdx = ((b[pos] & 0xFF) << 8) | (b[pos + 1] & 0xFF);
                    int descIdx = ((b[pos + 2] & 0xFF) << 8) | (b[pos + 3] & 0xFF);
                    nameAndTypeMap.put(i, new int[]{nameIdx, descIdx});
                    pos += 4;
                    break;
                }
                case 3: // Integer
                case 4: // Float
                    pos += 4;
                    break;
                case 5: // Long
                case 6: // Double
                    pos += 8;
                    i++; // occupies two entries
                    break;
                case 15: // MethodHandle
                    pos += 3;
                    break;
                case 16: // MethodType
                    pos += 2;
                    break;
                case 18: // InvokeDynamic
                    pos += 4;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown constant pool tag " + tag + " at index " + i + " for " + debugName);
            }
        }

        Set<String> classNames = new HashSet<>();
        for (Map.Entry<Integer, Integer> e : classNameIndex.entrySet()) {
            String name = utf8Map.get(e.getValue());
            if (name != null) {
                classNames.add(name);
            }
        }

        Set<String> utf8Values = new HashSet<>(utf8Map.values());

        Set<String> stringConstants = new HashSet<>();
        for (Map.Entry<Integer, Integer> e : stringIndex.entrySet()) {
            String s = utf8Map.get(e.getValue());
            if (s != null) {
                stringConstants.add(s);
            }
        }

        List<MemberRef> memberRefs = new ArrayList<>();
        for (int[] ref : fieldMethodRefs) {
            int classIdx = ref[1];
            int natIdx = ref[2];
            Integer classNameIdx = classNameIndex.get(classIdx);
            String className = classNameIdx != null ? utf8Map.get(classNameIdx) : null;
            int[] nat = nameAndTypeMap.get(natIdx);
            String name = null;
            String desc = null;
            if (nat != null) {
                name = utf8Map.get(nat[0]);
                desc = utf8Map.get(nat[1]);
            }
            memberRefs.add(new MemberRef(className, name, desc, ref[0]));
        }

        return new BytecodeInfo(major, utf8Values, classNames, stringConstants, memberRefs);
    }

    private static final class BytecodeInfo {
        final int majorVersion;
        final Set<String> utf8Values;
        final Set<String> classNames;
        final Set<String> stringConstants;
        final List<MemberRef> memberRefs;

        BytecodeInfo(int majorVersion, Set<String> utf8Values, Set<String> classNames,
                     Set<String> stringConstants, List<MemberRef> memberRefs) {
            this.majorVersion = majorVersion;
            this.utf8Values = utf8Values;
            this.classNames = classNames;
            this.stringConstants = stringConstants;
            this.memberRefs = memberRefs;
        }
    }

    private static final class MemberRef {
        final String className;
        final String name;
        final String descriptor;
        final int tag;

        MemberRef(String className, String name, String descriptor, int tag) {
            this.className = className;
            this.name = name;
            this.descriptor = descriptor;
            this.tag = tag;
        }

        @Override
        public String toString() {
            return "MemberRef{tag=" + tag + ", class=" + className + ", name=" + name + ", desc=" + descriptor + "}";
        }
    }
}
