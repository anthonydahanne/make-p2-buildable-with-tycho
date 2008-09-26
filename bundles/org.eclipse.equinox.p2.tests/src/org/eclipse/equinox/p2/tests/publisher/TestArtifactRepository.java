/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipInputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRequest;
import org.eclipse.equinox.internal.p2.artifact.repository.Messages;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepHandler;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.osgi.util.NLS;

@SuppressWarnings( {"restriction", "unchecked"})
public class TestArtifactRepository implements IArtifactRepository {
	private static String provider = null;
	private HashMap/*<IArtifactDescriptor, byte[]>*/repo;
	private String name;
	private String description;
	private String version = "1.0.0"; //$NON-NLS-1$
	protected Map properties = new OrderedProperties();

	public class ArtifactOutputStream extends OutputStream implements IStateful {
		private boolean closed;
		private long count = 0;
		private IArtifactDescriptor descriptor;
		private OutputStream destination;
		private IStatus status = Status.OK_STATUS;
		private OutputStream firstLink;

		public ArtifactOutputStream(OutputStream os, IArtifactDescriptor descriptor) {
			this.destination = os;
			this.descriptor = descriptor;
		}

		public void close() throws IOException {
			if (closed)
				return;
			try {
				destination.close();
				closed = true;
			} catch (IOException e) {
				if (getStatus().isOK())
					throw e;
				// if the stream has already been e.g. canceled, we can return -
				// the status is already set correctly
				return;
			}
			// if the steps ran ok and there was actual content, write the
			// artifact descriptor
			// TODO the count check is a bit bogus but helps in some error cases
			// (e.g., the optimizer)
			// where errors occurred in a processing step earlier in the chain.
			// We likely need a better
			// or more explicit way of handling this case.
			OutputStream testStream = firstLink == null ? this : firstLink;
			if (ProcessingStepHandler.checkStatus(testStream).isOK() && count > 0) {
				((ArtifactDescriptor) descriptor).setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, Long.toString(count));
				addDescriptor(descriptor, ((ByteArrayOutputStream) destination).toByteArray());
			}
		}

		public IStatus getStatus() {
			return status;
		}

		public OutputStream getDestination() {
			return destination;
		}

		public void setStatus(IStatus status) {
			this.status = status == null ? Status.OK_STATUS : status;
		}

		public void write(byte[] b) throws IOException {
			destination.write(b);
			count += b.length;
		}

		public void write(byte[] b, int off, int len) throws IOException {
			destination.write(b, off, len);
			count += len;
		}

		public void write(int b) throws IOException {
			destination.write(b);
			count++;
		}

		public void setFirstLink(OutputStream value) {
			firstLink = value;
		}
	}

	public TestArtifactRepository() {
		repo = new HashMap/*<IArtifactDescriptor, byte[]>*/();
	}

	public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
		// Check if the artifact is already in this repository
		if (contains(descriptor)) {
			String msg = NLS.bind(Messages.available_already_in, getLocation().toExternalForm());
			throw new ProvisionException(new Status(IStatus.ERROR, TestActivator.PI_PROV_TESTS, ProvisionException.ARTIFACT_EXISTS, msg, null));
		}
		return new ArtifactOutputStream(new ByteArrayOutputStream(500), descriptor);
	}

	public void addDescriptor(IArtifactDescriptor descriptor) {
		addDescriptor(descriptor, new byte[0]);
	}

	public void addDescriptor(IArtifactDescriptor descriptor, byte[] bytes) {
		repo.put(descriptor, bytes);
	}

	public void addDescriptors(IArtifactDescriptor[] descriptors) {
		for (int i = 0; i < descriptors.length; i++)
			addDescriptor(descriptors[i]);
	}

	public boolean contains(IArtifactDescriptor descriptor) {
		return repo.containsKey(descriptor);
	}

	public synchronized boolean contains(IArtifactKey key) {
		for (Iterator/*<IArtifactDescriptor>*/iterator = repo.keySet().iterator(); iterator.hasNext();) {
			IArtifactDescriptor descriptor = (IArtifactDescriptor) iterator.next();
			if (descriptor.getArtifactKey().equals(key))
				return true;
		}
		return false;
	}

	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		try {
			byte[] repoContents = (byte[]) repo.get(descriptor);
			if (repoContents == null)
				return null;
			destination.write(repoContents);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return Status.OK_STATUS;
	}

	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		Set/*<IArtifactDescriptor>*/result = new HashSet/*<IArtifactDescriptor>*/();
		for (Iterator/*<IArtifactDescriptor>*/iterator = repo.keySet().iterator(); iterator.hasNext();) {
			IArtifactDescriptor descriptor = (IArtifactDescriptor) iterator.next();
			if (descriptor.getArtifactKey().equals(key))
				result.add(descriptor);
		}
		return (IArtifactDescriptor[]) result.toArray(new IArtifactDescriptor[0]);
	}

	public IArtifactKey[] getArtifactKeys() {
		Set/*<IArtifactKey>*/result = new HashSet/*<IArtifactKey>*/();
		for (Iterator/*<IArtifactDescriptor>*/iterator = repo.keySet().iterator(); iterator.hasNext();) {
			IArtifactDescriptor descriptor = (IArtifactDescriptor) iterator.next();
			result.add(descriptor.getArtifactKey());
		}
		return (IArtifactKey[]) result.toArray(new IArtifactKey[0]);
	}

	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, requests.length);
		try {
			//plugin ID taken from TestActivator
			MultiStatus overallStatus = new MultiStatus("org.eclipse.equinox.p2.test", IStatus.OK, null, null); //$NON-NLS-1$
			for (int i = 0; i < requests.length; i++) {
				overallStatus.add(getArtifact((ArtifactRequest) requests[i], subMonitor.newChild(1)));
			}
			return (monitor.isCanceled() ? Status.CANCEL_STATUS : overallStatus);
		} finally {
			subMonitor.done();
		}
	}

	private IStatus getArtifact(ArtifactRequest artifactRequest, IProgressMonitor monitor) {
		artifactRequest.setSourceRepository(this);
		artifactRequest.perform(monitor);
		return artifactRequest.getResult();
	}

	public void removeDescriptor(IArtifactDescriptor descriptor) {
		repo.remove(descriptor);
	}

	public void removeDescriptor(IArtifactKey key) {
		ArrayList/*<IArtifactDescriptor>*/removeList = new ArrayList/*<IArtifactDescriptor>*/();
		for (Iterator/*<IArtifactDescriptor>*/iterator = repo.keySet().iterator(); iterator.hasNext();) {
			IArtifactDescriptor descriptor = (IArtifactDescriptor) iterator.next();
			if (descriptor.getArtifactKey().equals(key))
				removeList.add(descriptor);
		}
		for (int i = 0; i < repo.size(); i++) {
			repo.remove(removeList.get(i));
		}
	}

	public String getDescription() {
		return description;
	}

	public URL getLocation() {
		return null;
	}

	public String getName() {
		return name;
	}

	public Map getProperties() {
		return OrderedProperties.unmodifiableProperties(properties);
	}

	public String getProvider() {
		return provider;
	}

	public String getType() {
		return "memoryArtifactRepo"; //$NON-NLS-1$
	}

	public String getVersion() {
		return version;
	}

	public boolean isModifiable() {
		return true;
	}

	public void setDescription(String value) {
		this.description = value;
	}

	public void setName(String value) {
		this.name = value;
	}

	public String setProperty(String key, String value) {
		return (String) (value == null ? properties.remove(key) : properties.put(key, value));
	}

	public void setProvider(String value) {
		provider = value;
	}

	public void removeAll() {
		repo.clear();
	}

	public Object getAdapter(Class adapter) {
		return null;
	}

	public ZipInputStream getZipInputStream(IArtifactKey key) {
		//get first descriptor with key
		IArtifactDescriptor[] descriptor = getArtifactDescriptors(key);
		if (descriptor == null || descriptor.length == 0 || descriptor[0] == null)
			return null;
		return new ZipInputStream(new ByteArrayInputStream((byte[]) repo.get(descriptor[0]), 0, ((byte[]) repo.get(descriptor[0])).length));
	}

	public ZipInputStream getZipInputStream(IArtifactDescriptor descriptor) {
		return new ZipInputStream(new ByteArrayInputStream((byte[]) repo.get(descriptor), 0, ((byte[]) repo.get(descriptor)).length));
	}

	public byte[] getBytes(IArtifactDescriptor artifactDescriptor) {
		return (byte[]) repo.get(artifactDescriptor);
	}
}