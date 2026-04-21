package fr.xephi.authme.security.crypts;

import com.google.common.primitives.Ints;
import de.rtner.misc.BinTools;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;
import fr.xephi.authme.settings.Settings;

import javax.inject.Inject;

@Recommendation(Usage.RECOMMENDED)
public class Pbkdf2 extends AbstractPbkdf2 {

    private static final int DEFAULT_ROUNDS = 10_000;
    private final ConsoleLogger logger = ConsoleLoggerFactory.get(Pbkdf2.class);

    @Inject
    Pbkdf2(Settings settings) {
        super(settings, DEFAULT_ROUNDS);
    }

    @Override
    public String computeHash(String password, String salt, String name) {
        return "pbkdf2_sha256$" + numberOfRounds + "$" + salt + "$"
            + BinTools.bin2hex(deriveKey(password, salt.getBytes(), numberOfRounds, 64));
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String unusedName) {
        String[] line = hashedPassword.getHash().split("\\$");
        if (line.length != 4) {
            return false;
        }
        Integer iterations = Ints.tryParse(line[1]);
        if (iterations == null) {
            logger.warning("Cannot read number of rounds for Pbkdf2: '" + line[1] + "'");
            return false;
        }
        return verifyKey(password, line[2].getBytes(), iterations, BinTools.hex2bin(line[3]));
    }

    @Override
    public int getSaltLength() {
        return 16;
    }

}
