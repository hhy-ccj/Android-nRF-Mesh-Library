/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.meshprovisioner.transport;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import no.nordicsemi.android.meshprovisioner.MeshNetwork;
import no.nordicsemi.android.meshprovisioner.provisionerstates.UnprovisionedMeshNode;
import no.nordicsemi.android.meshprovisioner.utils.AddressUtils;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;
import no.nordicsemi.android.meshprovisioner.utils.SecureUtils;
import no.nordicsemi.android.meshprovisioner.utils.SparseIntArrayParcelable;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@SuppressWarnings("WeakerAccess")
@Entity(tableName = "nodes",
        foreignKeys = @ForeignKey(entity = MeshNetwork.class,
                parentColumns = "mesh_uuid",
                childColumns = "mesh_uuid",
                onUpdate = CASCADE, onDelete = CASCADE),
        indices = @Index("mesh_uuid"))
public final class ProvisionedMeshNode extends ProvisionedBaseMeshNode {

    @Ignore
    @Expose
    private SecureUtils.K2Output k2Output;

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public ProvisionedMeshNode() {
    }

    @Ignore
    public ProvisionedMeshNode(final UnprovisionedMeshNode unprovisionedMeshNode) {
        uuid = unprovisionedMeshNode.getDeviceUuid().toString();
        isProvisioned = unprovisionedMeshNode.isProvisioned();
        isConfigured = unprovisionedMeshNode.isConfigured();
        nodeName = unprovisionedMeshNode.getNodeName();
        networkKey = unprovisionedMeshNode.getNetworkKey();
        final NetworkKey networkKey = new NetworkKey(unprovisionedMeshNode.getKeyIndex(), unprovisionedMeshNode.getNetworkKey());
        networkKeys.add(networkKey);
        identityKey = unprovisionedMeshNode.getIdentityKey();
        mFlags = unprovisionedMeshNode.getFlags();
        unicastAddress = unprovisionedMeshNode.getUnicastAddress();
        deviceKey = unprovisionedMeshNode.getDeviceKey();
        ttl = unprovisionedMeshNode.getTtl();
        k2Output = SecureUtils.calculateK2(networkKey.getKey(), SecureUtils.K2_MASTER_INPUT);
        mTimeStampInMillis = unprovisionedMeshNode.getTimeStamp();
        mConfigurationSrc = unprovisionedMeshNode.getConfigurationSrc();
        numberOfElements = unprovisionedMeshNode.getNumberOfElements();
    }

    @Ignore
    protected ProvisionedMeshNode(Parcel in) {
        uuid = in.readString();
        isProvisioned = in.readByte() != 0;
        isConfigured = in.readByte() != 0;
        nodeName = in.readString();
        networkKeys = in.readArrayList(NetworkKey.class.getClassLoader());
        mFlags = in.createByteArray();
        unicastAddress = in.createByteArray();
        deviceKey = in.createByteArray();
        ttl = in.readInt();
        numberOfElements = in.readInt();
        mReceivedSequenceNumber = in.readInt();
        k2Output = in.readParcelable(SecureUtils.K2Output.class.getClassLoader());
        companyIdentifier = (Integer) in.readValue(Integer.class.getClassLoader());
        productIdentifier = (Integer) in.readValue(Integer.class.getClassLoader());
        versionIdentifier = (Integer) in.readValue(Integer.class.getClassLoader());
        crpl = (Integer) in.readValue(Integer.class.getClassLoader());
        features = (Integer) in.readValue(Integer.class.getClassLoader());
        relayFeatureSupported = (Boolean) in.readValue(Boolean.class.getClassLoader());
        proxyFeatureSupported = (Boolean) in.readValue(Boolean.class.getClassLoader());
        friendFeatureSupported = (Boolean) in.readValue(Boolean.class.getClassLoader());
        lowPowerFeatureSupported = (Boolean) in.readValue(Boolean.class.getClassLoader());
        generatedNetworkId = in.createByteArray();
        sortElements(in.readHashMap(Element.class.getClassLoader()));
        mAddedApplicationKeys = in.readHashMap(ApplicationKey.class.getClassLoader());
        mAddedAppKeyIndexes = in.readArrayList(Integer.class.getClassLoader());
        mTimeStampInMillis = in.readLong();
        mConfigurationSrc = in.createByteArray();
        mSeqAuth = in.readParcelable(SparseIntArrayParcelable.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uuid);
        dest.writeByte((byte) (isProvisioned ? 1 : 0));
        dest.writeByte((byte) (isConfigured ? 1 : 0));
        dest.writeString(nodeName);
        dest.writeList(networkKeys);
        dest.writeByteArray(mFlags);
        dest.writeByteArray(unicastAddress);
        dest.writeByteArray(deviceKey);
        dest.writeInt(ttl);
        dest.writeInt(numberOfElements);
        dest.writeInt(mReceivedSequenceNumber);
        dest.writeParcelable(k2Output, flags);
        dest.writeValue(companyIdentifier);
        dest.writeValue(productIdentifier);
        dest.writeValue(versionIdentifier);
        dest.writeValue(crpl);
        dest.writeValue(features);
        dest.writeValue(relayFeatureSupported);
        dest.writeValue(proxyFeatureSupported);
        dest.writeValue(friendFeatureSupported);
        dest.writeValue(friendFeatureSupported);
        dest.writeByteArray(generatedNetworkId);
        dest.writeMap(mElements);
        dest.writeMap(mAddedApplicationKeys);
        dest.writeList(mAddedAppKeyIndexes);
        dest.writeLong(mTimeStampInMillis);
        dest.writeByteArray(mConfigurationSrc);
        dest.writeParcelable(mSeqAuth, flags);
    }


    public static final Creator<ProvisionedMeshNode> CREATOR = new Creator<ProvisionedMeshNode>() {
        @Override
        public ProvisionedMeshNode createFromParcel(Parcel in) {
            return new ProvisionedMeshNode(in);
        }

        @Override
        public ProvisionedMeshNode[] newArray(int size) {
            return new ProvisionedMeshNode[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    public final int getTtl() {
        return ttl;
    }

    public final void setTtl(final int ttl) {
        this.ttl = ttl;
    }

    public final Map<Integer, Element> getElements() {
        return mElements;
    }

    public final void setElements(final Map<Integer, Element> elements) {
        mElements = elements;
    }

    public final byte[] getDeviceKey() {
        return deviceKey;
    }

    public void setDeviceKey(final byte[] deviceKey) {
        this.deviceKey = deviceKey;
    }

    public final int getReceivedSequenceNumber() {
        return mReceivedSequenceNumber;
    }

    public final void setReceivedSequenceNumber(final int receivedSequenceNumber) {
        mReceivedSequenceNumber = receivedSequenceNumber;
    }

    public final SecureUtils.K2Output getK2Output() {
        return k2Output;
    }

    final void setK2Output(final SecureUtils.K2Output k2Output) {
        this.k2Output = k2Output;
    }

    public final Integer getCompanyIdentifier() {
        return companyIdentifier;
    }

    public final void setCompanyIdentifier(final Integer companyIdentifier) {
        this.companyIdentifier = companyIdentifier;
    }

    public final Integer getProductIdentifier() {
        return productIdentifier;
    }

    public final void setProductIdentifier(final Integer productIdentifier) {
        this.productIdentifier = productIdentifier;
    }

    public final Integer getVersionIdentifier() {
        return versionIdentifier;
    }

    public final void setVersionIdentifier(final Integer versionIdentifier) {
        this.versionIdentifier = versionIdentifier;
    }

    public final Integer getCrpl() {
        return crpl;
    }

    public final void setCrpl(final Integer crpl) {
        this.crpl = crpl;
    }

    public final Integer getFeatures() {
        return features;
    }

    public final Boolean isRelayFeatureSupported() {
        return relayFeatureSupported;
    }

    public final void setRelayFeatureSupported(final Boolean supported) {
        relayFeatureSupported = supported;
    }

    public final Boolean isProxyFeatureSupported() {
        return proxyFeatureSupported;
    }

    public final void setProxyFeatureSupported(final Boolean supported) {
        proxyFeatureSupported = supported;
    }

    public final Boolean isFriendFeatureSupported() {
        return friendFeatureSupported;
    }

    public final void setFriendFeatureSupported(final Boolean supported) {
        friendFeatureSupported = supported;
    }

    public final Boolean isLowPowerFeatureSupported() {
        return lowPowerFeatureSupported;
    }

    public final void setLowPowerFeatureSupported(final Boolean supported) {
        lowPowerFeatureSupported = supported;
    }

    public int getNumberOfElements() {
        return numberOfElements;
    }

    public final Map<Integer, String> getTempAddedAppKeys() {
        return Collections.unmodifiableMap(mAddedAppKeys);
    }

    public final Map<Integer, ApplicationKey> getAddedApplicationKeys() {
        return Collections.unmodifiableMap(mAddedApplicationKeys);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public final void setAddedApplicationKeys(final Map<Integer, ApplicationKey> applicationKeys){
        mAddedApplicationKeys = applicationKeys;
    }

    protected final void setAddedAppKey(final int index, final ApplicationKey appKey) {
        this.mAddedApplicationKeys.put(index, appKey);
    }

    /**
     * Sets the data from the {@link ConfigCompositionDataStatus}
     *
     * @param configCompositionDataStatus Composition data status object
     */
    protected final void setCompositionData(@NonNull final ConfigCompositionDataStatus configCompositionDataStatus) {
        if (configCompositionDataStatus != null) {
            companyIdentifier = configCompositionDataStatus.getCompanyIdentifier();
            productIdentifier = configCompositionDataStatus.getProductIdentifier();
            versionIdentifier = configCompositionDataStatus.getVersionIdentifier();
            crpl = configCompositionDataStatus.getCrpl();
            features = configCompositionDataStatus.getFeatures();
            relayFeatureSupported = configCompositionDataStatus.isRelayFeatureSupported();
            proxyFeatureSupported = configCompositionDataStatus.isProxyFeatureSupported();
            friendFeatureSupported = configCompositionDataStatus.isFriendFeatureSupported();
            lowPowerFeatureSupported = configCompositionDataStatus.isLowPowerFeatureSupported();
            mElements.putAll(configCompositionDataStatus.getElements());
        }
    }

    /**
     * Sets the bound app key data from the {@link ConfigModelAppStatus}
     *
     * @param configModelAppStatus ConfigModelAppStatus containing the bound app key information
     */
    protected final void setAppKeyBindStatus(@NonNull final ConfigModelAppStatus configModelAppStatus) {
        if (configModelAppStatus != null) {
            if (configModelAppStatus.isSuccessful()) {
                final Element element = mElements.get(configModelAppStatus.getElementAddress());
                final int modelIdentifier = configModelAppStatus.getModelIdentifier();
                final MeshModel model = element.getMeshModels().get(modelIdentifier);
                final int appKeyIndex = configModelAppStatus.getAppKeyIndex();
                final ApplicationKey appKey = mAddedApplicationKeys.get(appKeyIndex);
                model.setBoundAppKey(appKeyIndex, appKey);
            }
        }
    }

    /**
     * Sets the unbind app key data from the {@link ConfigModelAppStatus}
     *
     * @param configModelAppStatus ConfigModelAppStatus containing the unbound app key information
     */
    protected final void setAppKeyUnbindStatus(@NonNull final ConfigModelAppStatus configModelAppStatus) {
        if (configModelAppStatus != null) {
            if (configModelAppStatus.isSuccessful()) {
                final Element element = mElements.get(configModelAppStatus.getElementAddress());
                final int modelIdentifier = configModelAppStatus.getModelIdentifier();
                final MeshModel model = element.getMeshModels().get(modelIdentifier);
                final int appKeyIndex = configModelAppStatus.getAppKeyIndex();
                model.removeBoundAppKey(appKeyIndex);
            }

        }
    }

    private void sortElements(final HashMap<Integer, Element> unorderedElements) {
        final Set<Integer> unorderedKeys = unorderedElements.keySet();

        final List<Integer> orderedKeys = new ArrayList<>(unorderedKeys);
        Collections.sort(orderedKeys);
        for (int key : orderedKeys) {
            mElements.put(key, unorderedElements.get(key));
        }
    }

    public void setSeqAuth(final byte[] src, final int seqAuth) {
        final int srcAddress = AddressUtils.getUnicastAddressInt(src);
        mSeqAuth.put(srcAddress, seqAuth);
    }

    public Integer getSeqAuth(final byte[] src) {
        if (mSeqAuth.size() == 0) {
            return null;
        }

        final int srcAddress = AddressUtils.getUnicastAddressInt(src);
        return mSeqAuth.get(srcAddress);
    }

    /**
     * Method for migrating old network key data
     */
    @SuppressWarnings("unused")
    private void tempMigrateNetworkKey() {
        if (networkKey != null) {
            netKeyIndex = MeshParserUtils.removeKeyIndexPadding(keyIndex);
            NetworkKey netKey = new NetworkKey(netKeyIndex, networkKey);
            networkKeys.add(netKey);
        }
    }

    /**
     * Method for migrating old Application key data
     */
    @SuppressWarnings("unused")
    private void tempMigrateAddedApplicationKeys() {
        for (Map.Entry<Integer, String> entry : mAddedAppKeys.entrySet()) {
            if (entry.getValue() != null) {
                final ApplicationKey applicationKey = new ApplicationKey(entry.getKey(), MeshParserUtils.toByteArray(entry.getValue()));
                mAddedApplicationKeys.put(applicationKey.getKeyIndex(), applicationKey);
            }
        }
    }

    /**
     * Method for migrating old Application key data
     */
    @SuppressWarnings("unused")
    private void tempMigrateBoundApplicationKeys() {
        for (Map.Entry<Integer, Element> elementEntry : mElements.entrySet()) {
            if (elementEntry.getValue() != null) {
                final Element element = elementEntry.getValue();
                for (Map.Entry<Integer, MeshModel> modelEntry : element.getMeshModels().entrySet()) {
                    if (modelEntry.getValue() != null) {
                        final MeshModel meshModel = modelEntry.getValue();
                        for (Map.Entry<Integer, String> appKeyEntry : meshModel.getBoundAppkeys().entrySet()) {
                            final int keyIndex = appKeyEntry.getKey();
                            final byte[] key = MeshParserUtils.toByteArray(appKeyEntry.getValue());
                            final ApplicationKey applicationKey = new ApplicationKey(keyIndex, key);
                            //meshModel.mBoundApplicationKeys.put(keyIndex, applicationKey);
                        }
                    }
                }
            }
        }
    }

    /**
     * Method for migrating old Application key data
     */
    @SuppressWarnings("unused")
    private void tempMigrateSubscriptions() {
        for (Map.Entry<Integer, Element> elementEntry : mElements.entrySet()) {
            if (elementEntry.getValue() != null) {
                final Element element = elementEntry.getValue();
                for (Map.Entry<Integer, MeshModel> modelEntry : element.getMeshModels().entrySet()) {
                    if (modelEntry.getValue() != null) {
                        final MeshModel meshModel = modelEntry.getValue();
                        for (int i = 0; i < meshModel.getSubscriptionAddresses().size(); i++) {
                            final byte[] address = meshModel.getSubscriptionAddresses().get(i);
                            meshModel.mSubscriptionAddress.add(address);
                        }
                    }
                }
            }
        }
    }
}