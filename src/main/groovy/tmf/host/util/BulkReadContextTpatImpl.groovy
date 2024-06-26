package tmf.host.util

import edu.gatech.gtri.trustmark.v1_0.FactoryLoader
import edu.gatech.gtri.trustmark.v1_0.impl.io.bulk.BulkImportUtils
import edu.gatech.gtri.trustmark.v1_0.impl.io.bulk.BulkReadContextImpl
import edu.gatech.gtri.trustmark.v1_0.impl.model.TrustmarkFrameworkIdentifiedObjectImpl
import edu.gatech.gtri.trustmark.v1_0.impl.service.ServiceReferenceNameResolver
import edu.gatech.gtri.trustmark.v1_0.model.Entity
import edu.gatech.gtri.trustmark.v1_0.model.TrustmarkFrameworkIdentifiedObject
import edu.gatech.gtri.trustmark.v1_0.service.TrustmarkFrameworkService
import edu.gatech.gtri.trustmark.v1_0.service.TrustmarkFrameworkServiceFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tmf.host.VersionSet
import tmf.host.VersionSetTDLink
import tmf.host.VersionSetTIPLink
import org.apache.commons.lang.StringUtils

class BulkReadContextTpatImpl extends BulkReadContextImpl {

    /**
     * Resolver class for finding a TrustInteroperabilityProfile based on a version set and an identifier.
     * This class implements the VersionSetObjectNameResolver interface and is used to encapsulate the logic
     * for retrieving trust interoperability profiles by their identifiers associated with a specific version set.
     */
    public class TipVersionSetByIdentifierResolver implements VersionSetObjectNameResolver {

        /**
         * Resolves a trust interoperability profile by its identifier within a given version set.
         *
         * @param vs the version set within which to search for the trust interoperability profile
         * @param identifier the identifier of the trust interoperability profile to find
         * @return the found trust interoperability profile or {@code null} if no profile matches the identifier
         */
        @Override
        public Object resolve(VersionSet vs, String identifier) {
            return VersionSetTIPLink.findByVersionSetAndTipIdentifier(vs, identifier)?.trustInteroperabilityProfile;
        }
    }

    /**
     * Resolver class for finding a TrustInteroperabilityProfile based on a version set and a name.
     * This class implements the VersionSetObjectNameResolver interface and is used to encapsulate the logic for
     * retrieving trust interoperability profiles by their names associated with a specific version set.
     */
    public class TipVersionSetByNameResolver implements VersionSetObjectNameResolver {

        /**
         * Resolves a trust interoperability profile by its name within a given version set.
         *
         * @param vs the version set within which to search for the trust interoperability profile
         * @param name the name of the trust interoperability profile to find
         * @return the found trust interoperability profile or {@code null} if no profile matches the name
         */
        @Override
        public Object resolve(VersionSet vs, String name) {
            return VersionSetTIPLink.findAllByVersionSet(vs)?.find { it?.trustInteroperabilityProfile?.name == name }?.trustInteroperabilityProfile;
        }
    }

    /**
     * Resolver class for finding a TrustmarkDefinition based on a version set and an identifier.
     * This class implements the VersionSetObjectNameResolver interface and is used to encapsulate the logic
     * for retrieving trustmark definitions by their identifiers associated with a specific version set.
     */
    public class TdVersionSetByIdentifierResolver implements VersionSetObjectNameResolver {

        /**
         * Resolves a trustmark definition by its identifier within a given version set.
         *
         * @param vs the version set within which to search for the trustmark definition
         * @param identifier the identifier of the trustmark definition to find
         * @return the found trustmark definition or {@code null} if no profile matches the identifier
         */
        @Override
        public Object resolve(VersionSet vs, String identifier) {
            return VersionSetTDLink.findByVersionSetAndTdIdentifier(vs, identifier)?.trustmarkDefinition;
        }
    }

    /**
     * Resolver class for finding a TrustmarkDefinition based on a version set and a name.
     * This class implements the VersionSetObjectNameResolver interface and is used to encapsulate the logic for
     * retrieving trustmark definitions by their names associated with a specific version set.
     */
    public class TdVersionSetByNameResolver implements VersionSetObjectNameResolver {

        /**
         * Resolves a trustmark definition by its name within a given version set.
         *
         * @param vs the version set within which to search for the trustmark definition
         * @param name the name of the trustmark definition to find
         * @return the found trustmark definition or {@code null} if no trustmark definition matches the name
         */
        @Override
        public Object resolve(VersionSet vs, String name) {
            return VersionSetTDLink.findAllByVersionSet(vs)?.find{ it?.trustmarkDefinition?.name == name }?.trustmarkDefinition;
        }
    }


    private static final Logger log = LoggerFactory.getLogger(BulkReadContextTpatImpl.class)

    @Override
    Entity getTrustmarkDefiningOrganization() {
        //Switching to Default TD Entity from DB
        return TFAMPropertiesHolder.getDefaultEntity();
    }

    @Override
    Entity getTrustInteroperabilityProfileIssuer() {
        //Switching to Default TIP Entity from DB
        return TFAMPropertiesHolder.getDefaultEntity();
    }

    @Override
    List<Entity> getTrustmarkProviderReferences() {
        List<URL> providerRefIds = TFAMPropertiesHolder.getProviderReferences()
        log.debug("getTrustmarkProviderReferences providerRefIds: " + providerRefIds)
        if( providerRefIds?.size() > 0 ){
            List providers = []
            for( URL id : providerRefIds ){
                DefaultEntityImpl entity = new DefaultEntityImpl()
                entity.setIdentifier(id.toURI())
                providers.add(entity)
            }
            return providers
        }else{
            return []
        }
    }

    @Override
    URI generateIdentifierForTrustmarkDefinition(String moniker, String version) throws URISyntaxException {
        String uriString = String.format("%s/%s/%s/", TFAMPropertiesHolder.getTdIdentifierUriBase(), moniker, version);
        return new URI(uriString);
    }

    @Override
    URI generateIdentifierForTrustInteroperabilityProfile(String moniker, String version) throws URISyntaxException {
        String uriString = String.format("%s/%s/%s/", TFAMPropertiesHolder.getTipIdentifierUriBase(), moniker, version);
        return new URI(uriString);
    }


    @Override
    TrustmarkFrameworkIdentifiedObject resolveReferencedExternalTrustmarkDefinition(String tdReference) {
        log.debug("Resolving Referenced External TrustmarkDefinition {}", tdReference)
        if(BulkImportUtils.isValidUri(tdReference)) {
            return resolveReferencedExternalArtifact(
                    tdReference,
                    new TdVersionSetByIdentifierResolver(),
                    new TdVersionSetByNameResolver(),
                    { tfs, ref -> tfs.getTrustmarkDefinitionByUrl(ref) }
            )
        }
        return resolveReferencedExternalArtifact(
                tdReference,
                new TdVersionSetByIdentifierResolver(),
                new TdVersionSetByNameResolver(),
                { tfs, ref -> tfs.getTrustmarkDefinitionByName(ref) }
        )
    }

    @Override
    TrustmarkFrameworkIdentifiedObject resolveReferencedExternalTrustInteroperabilityProfile(String tipReference) {
        log.debug("Resolving Referenced External TrustInteroperabilityProfile {}", tipReference)
        if(BulkImportUtils.isValidUri(tipReference)) {
            return resolveReferencedExternalArtifact(
                    tipReference,
                    new TipVersionSetByIdentifierResolver(),
                    new TipVersionSetByNameResolver(),
                    { tfs, ref -> tfs.getTrustInteroperabilityProfileByUrl(ref) }
            )
        }
        return resolveReferencedExternalArtifact(
            tipReference,
            new TipVersionSetByIdentifierResolver(),
            new TipVersionSetByNameResolver(),
            { tfs, ref -> tfs.getTrustInteroperabilityProfileByName(ref) }
        )
    }

    private static TrustmarkFrameworkIdentifiedObject resolveReferencedExternalArtifact(
        String reference,
        VersionSetObjectNameResolver finderByIdentifier,
        VersionSetObjectNameResolver finderByName,
        ServiceReferenceNameResolver serviceReferenceNameResolver
    ) {
        TrustmarkFrameworkIdentifiedObject result

        log.debug("Resolving Referenced External Artifact. Reference for LookUp ->  " + reference)

        // check for an ID or name in the current version set
        VersionSet currentVs = null
        VersionSet.withTransaction {
            currentVs = VersionSet.findByProduction(true)
            log.debug("Reference for LookUp ->  "+reference+"  Current Version Set -> " + currentVs)
            if (currentVs == null) {
                currentVs = VersionSet.findByDevelopment(true)  // get the first version set, since there isn't a production one
            }
            Object tfo = finderByIdentifier.resolveToObject(currentVs, reference)
            if(tfo != null)
            {
                result = new TrustmarkFrameworkIdentifiedObjectImpl()
                result.identifier = new URI(tfo.identifier)
                result.name = tfo.name
                result.description = tfo.description
                result.version = tfo.ver
            }
        }
        if (result != null) {
            log.debug("Resolved reference by Identifier in current Version Set: " + result.version +":" + result.name +":" + result.description+":" + result.identifier)
            return result
        }
        VersionSet.withTransaction {
            Object tfo = finderByName.resolveToObject(currentVs, reference)
              if(tfo != null)
              {
                  result =  new TrustmarkFrameworkIdentifiedObjectImpl()
                  result.identifier = new URI(tfo.identifier)
                  result.name = tfo.name
                  result.description = tfo.description
                  result.version = tfo.ver
              }
        }
        if (result != null) {
            log.debug("Resolved reference by Name in current Version Set: " + result.version +":" + result.name +":" + result.description+":" + result.identifier)
            return result
        }
        
        // if not found, check the registry/registries for a match
        TrustmarkFrameworkServiceFactory tfsFactory = FactoryLoader.getInstance(TrustmarkFrameworkServiceFactory.class);
        URL resolvingRegistry = null
        for (URL registryUrl : TFAMPropertiesHolder.getRegistryUrls()) {
            TrustmarkFrameworkService tfs = tfsFactory.createService(registryUrl?.toString())
            log.debug(String.format("Registry URL -> %s ", registryUrl))
            try {
                TrustmarkFrameworkIdentifiedObject tfido = serviceReferenceNameResolver.resolve(tfs, reference)
                if(tfido != null)
                {
                    log.debug(String.format("Returned tfido %s %s %s %s\n",tfido.getIdentifier().toString(), tfido.getName(), tfido.getVersion(), tfido.getDescription()));
                    result =  new TrustmarkFrameworkIdentifiedObjectImpl()
                    result.identifier = new URI(tfido.getIdentifier().toString())
                    result.name = tfido.getName()
                    result.description = tfido.getDescription()
                    result.version = tfido.getVersion()
                    result.number = tfido.getNumber()
                }
            }
            catch (Exception ex) {
                log.error("Error resolving external artifact.", ex);
            }
            if (result != null) {
                resolvingRegistry = registryUrl
                break
            }
        }
        if (result != null) {
            log.debug(String.format("Resolved reference[%s] using registry[%s]", result, resolvingRegistry))
            return result
        }
        
        // try resolving as a URL (but return the URI object)
        URI tfUri = BulkImportUtils.getValidUrlAsUriOrNull(reference)
        if (tfUri == null) {
            log.debug("Unable to resolve reference: " + reference)
        } else {
            log.debug("Resolved reference as bare URI: " + tfUri)
            TrustmarkFrameworkService tfs = tfsFactory.createService(TFAMPropertiesHolder.getBaseUrlAsString());
            result =  serviceReferenceNameResolver.resolve(tfs, reference)
        }
        
        return result
    }


    @Override
    String getDefaultVersion() {
        return TFAMPropertiesHolder.getDefaultVersion()
    }

    @Override
    String getDefaultTipLegalNotice() {
        return TFAMPropertiesHolder.getDefaultTipLegalNotice()
    }

    @Override
    String getDefaultTipNotes() {
        return TFAMPropertiesHolder.getDefaultTipNotes()
    }

    @Override
    String getDefaultTdLegalNotice() {
        return TFAMPropertiesHolder.getDefaultTdLegalNotice()
    }

    @Override
    String getDefaultTdNotes() {
        return TFAMPropertiesHolder.getDefaultTdNotes()
    }

    @Override
    String getDefaultIssuanceCriteria() {
        return TFAMPropertiesHolder.getDefaultIssuanceCriteria()
    }

    @Override
    String getDefaultRevocationCriteria() {
        return TFAMPropertiesHolder.getDefaultRevocationCriteria()
    }

    @Override
    String getDefaultTargetStakeholderDescription() {
        return TFAMPropertiesHolder.getDefaultTargetStakeholderDescription()
    }

    @Override
    String getDefaultTargetRecipientDescription() {
        return TFAMPropertiesHolder.getDefaultTargetRecipientDescription()
    }

    @Override
    String getDefaultTargetRelyingPartyDescription() {
        return TFAMPropertiesHolder.getDefaultTargetRelyingPartyDescription()
    }

    @Override
    String getDefaultTargetProviderDescription() {
        return TFAMPropertiesHolder.getDefaultTargetProviderDescription()
    }

    @Override
    String getDefaultProviderEligibilityCriteria() {
        return TFAMPropertiesHolder.getDefaultProviderEligibilityCriteria()
    }

    @Override
    String getDefaultAssessorQualificationsDescription() {
        return TFAMPropertiesHolder.getDefaultAssessorQualificationsDescription()
    }

    @Override
    String getDefaultExtensionDescription() {
        return TFAMPropertiesHolder.getDefaultExtensionDescription()
    }

}
