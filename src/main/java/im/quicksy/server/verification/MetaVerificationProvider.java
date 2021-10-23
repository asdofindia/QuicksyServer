package im.quicksy.server.verification;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import im.quicksy.server.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class MetaVerificationProvider implements VerificationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetaVerificationProvider.class);

    final List<ProviderWrapper> providerList;

    public MetaVerificationProvider() {
        final TreeMap<String, Configuration.ProviderConfiguration> provider = Configuration.getInstance().getProvider();
        ImmutableList.Builder<ProviderWrapper> providerListBuilder = ImmutableList.builder();
        for(final Map.Entry<String,Configuration.ProviderConfiguration> entry : provider.entrySet()) {
            final String className = entry.getKey();
            final Configuration.ProviderConfiguration configuration = entry.getValue();
            final Class<? extends AbstractVerificationProvider> clazz;
            try {
                clazz = (Class<? extends AbstractVerificationProvider>) Class.forName(className);
            } catch (ClassNotFoundException | ClassCastException e) {
                LOGGER.warn("No VerificationProvider found matching for name {}", className);
                continue;
            }
            final AbstractVerificationProvider providerInstance;
            try {
                Constructor<? extends AbstractVerificationProvider> constructor = clazz.getConstructor(Map.class);
                providerInstance = constructor.newInstance(configuration.getParameter());
            } catch (NoSuchMethodException e) {
                LOGGER.warn("{} does not implement Map<String,String> constructor", clazz.getName());
                continue;
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                LOGGER.warn("Unable to construct VerificationProvider",e);
                continue;
            }
            providerListBuilder.add(new ProviderWrapper(configuration.getDeny(), configuration.getPattern(), providerInstance));
            LOGGER.info("found provider {} ", className);
        }
        final ImmutableList<ProviderWrapper> providerList = providerListBuilder.build();
        LOGGER.info("Found {} providers", providerList.size());
        if (providerList.size() == 0) {
            throw new IllegalStateException("No VerificationProviders found");
        }
        this.providerList = providerList;
    }

    @Override
    public boolean verify(Phonenumber.PhoneNumber phoneNumber, String pin) throws RequestFailedException {
        return getVerificationProvider(phoneNumber).verify(phoneNumber, pin);
    }

    @Override
    public void request(Phonenumber.PhoneNumber phoneNumber, Method method) throws RequestFailedException {
        getVerificationProvider(phoneNumber).request(phoneNumber, method);
    }

    @Override
    public void request(Phonenumber.PhoneNumber phoneNumber, Method method, String language) throws RequestFailedException {
        getVerificationProvider(phoneNumber).request(phoneNumber, method, language);
    }

    private AbstractVerificationProvider getVerificationProvider(Phonenumber.PhoneNumber phoneNumber) throws RequestFailedException {
        final int countryCode = phoneNumber.getCountryCode();
        final String e164 = PhoneNumberUtil.getInstance().format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
        for(final ProviderWrapper providerWrapper : this.providerList) {
            if (providerWrapper.reject(e164) || providerWrapper.deny.contains(countryCode)) {
                continue;
            }
            return providerWrapper.provider;
        }
        throw new RequestFailedException(String.format("No Verification Provider found to handle phone number %s", e164));
    }

    private static class ProviderWrapper {
        private final List<Integer> deny;
        private final Pattern pattern;
        private final AbstractVerificationProvider provider;

        private ProviderWrapper(List<Integer> deny, Pattern pattern, AbstractVerificationProvider provider) {
            this.deny = Preconditions.checkNotNull(deny);
            this.pattern = pattern;
            this.provider = Preconditions.checkNotNull(provider);
        }

        public boolean reject(final String e164) {
            return pattern != null && !pattern.matcher(e164).matches();
        }
    }
}
