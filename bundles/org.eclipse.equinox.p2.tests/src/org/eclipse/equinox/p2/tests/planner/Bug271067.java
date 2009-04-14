package org.eclipse.equinox.p2.tests.planner;

import java.io.File;
import java.lang.reflect.Field;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

public class Bug271067 extends AbstractProvisioningTest {
	private IProfile profile;
	private File previousStoreValue = null;
	String profileLoadedId = "bootProfile";
	IMetadataRepository repo = null;

	protected void setUp() throws Exception {
		super.setUp();
		File reporegistry1 = getTestData("test data bug 271067", "testData/bug271067/p2/org.eclipse.equinox.p2.engine/profileRegistry");
		File tempFolder = getTempFolder();
		copy("0.2", reporegistry1, tempFolder);
		SimpleProfileRegistry realProfileRegistry = (SimpleProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());
		//Tweak the running profile registry
		Field profilesMapField = SimpleProfileRegistry.class.getDeclaredField("profiles"); //$NON-NLS-1$
		profilesMapField.setAccessible(true);
		profilesMapField.set(realProfileRegistry, null);

		Field profileStore = SimpleProfileRegistry.class.getDeclaredField("store");
		profileStore.setAccessible(true);
		previousStoreValue = (File) profileStore.get(realProfileRegistry);
		profileStore.set(realProfileRegistry, tempFolder);
		//End of tweaking the profile registry

		profile = realProfileRegistry.getProfile(profileLoadedId);
		assertNotNull(profile);
		repo = loadMetadataRepository(getTestData("Repository for 271067", "testData/bug271067/").toURI());
	}

	@Override
	protected void tearDown() throws Exception {
		SimpleProfileRegistry realProfileRegistry = (SimpleProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());

		Field profilesMapField = SimpleProfileRegistry.class.getDeclaredField("profiles"); //$NON-NLS-1$
		profilesMapField.setAccessible(true);
		profilesMapField.set(realProfileRegistry, null);

		Field profileStore = SimpleProfileRegistry.class.getDeclaredField("store");
		profileStore.setAccessible(true);
		previousStoreValue = (File) profileStore.get(realProfileRegistry);
		profileStore.set(realProfileRegistry, previousStoreValue);
		super.tearDown();
	}

	IInstallableUnit getIU(IMetadataRepository source, String id, String version) {
		Collector c = repo.query(new InstallableUnitQuery(id, new Version(version)), new Collector(), new NullProgressMonitor());
		assertEquals(1, c.size());
		return (IInstallableUnit) c.iterator().next();
	}

	public void testInstallFeaturePatch() {
		// hello.feature.2.feature.group 1.0.0, , hello 1.0.2, hello 1.0.0,  hello.feature.2.feature.jar 1.0.0]
		ProfileChangeRequest installFeature1 = new ProfileChangeRequest(getProfile(profileLoadedId));
		IInstallableUnit featureGroup = getIU(repo, "hello.feature.1.feature.group", "1.0.0");
		IInstallableUnit featureJar = getIU(repo, "hello.feature.1.feature.jar", "1.0.0");
		IInstallableUnit helloIU = getIU(repo, "hello", "1.0.0");
		IInstallableUnit patch = getIU(repo, "hello.patch.feature.group", "1.0.0");
		IInstallableUnit helloPatch = getIU(repo, "hello", "1.0.0.1");
		IInstallableUnit patchJar = getIU(repo, "hello.patch.feature.jar", "1.0.0");

		installFeature1.addInstallableUnits(new IInstallableUnit[] {featureGroup, featureJar, helloIU, patch, helloPatch, patchJar});

		installFeature1.setInstallableUnitInclusionRules(featureGroup, PlannerHelper.createOptionalInclusionRule(featureGroup));
		installFeature1.setInstallableUnitInclusionRules(featureJar, PlannerHelper.createOptionalInclusionRule(featureJar));
		installFeature1.setInstallableUnitInclusionRules(helloIU, PlannerHelper.createOptionalInclusionRule(helloIU));
		installFeature1.setInstallableUnitInclusionRules(patch, PlannerHelper.createOptionalInclusionRule(patch));
		installFeature1.setInstallableUnitInclusionRules(helloPatch, PlannerHelper.createOptionalInclusionRule(helloPatch));
		installFeature1.setInstallableUnitInclusionRules(patchJar, PlannerHelper.createOptionalInclusionRule(patchJar));

		ProvisioningPlan feature1Plan = createPlanner().getProvisioningPlan(installFeature1, new ProvisioningContext(), null);
		assertOK("installation of feature1 and patch", createEngine().perform(getProfile(profileLoadedId), new DefaultPhaseSet(), feature1Plan.getOperands(), new ProvisioningContext(), new NullProgressMonitor()));
		assertEquals(1, getProfile(profileLoadedId).query(new InstallableUnitQuery("hello", new Version("1.0.0.1")), new Collector(), new NullProgressMonitor()).size());

		IInstallableUnit featureGroup2 = getIU(repo, "hello.feature.2.feature.group", "1.0.0");
		IInstallableUnit helloIU2 = getIU(repo, "hello", "1.0.2");
		IInstallableUnit featureJar2 = getIU(repo, "hello.feature.2.feature.jar", "1.0.0");

		ProfileChangeRequest installFeature2 = new ProfileChangeRequest(getProfile(profileLoadedId));
		installFeature2.addInstallableUnits(new IInstallableUnit[] {featureGroup2, helloIU2, featureJar2});
		installFeature2.setInstallableUnitInclusionRules(featureGroup2, PlannerHelper.createOptionalInclusionRule(featureGroup2));
		installFeature2.setInstallableUnitInclusionRules(helloIU2, PlannerHelper.createOptionalInclusionRule(helloIU2));
		installFeature2.setInstallableUnitInclusionRules(featureJar2, PlannerHelper.createOptionalInclusionRule(featureJar2));

		ProvisioningPlan feature2Plan = createPlanner().getProvisioningPlan(installFeature2, new ProvisioningContext(), null);
		assertOK("installation of feature2", createEngine().perform(getProfile(profileLoadedId), new DefaultPhaseSet(), feature2Plan.getOperands(), new ProvisioningContext(), new NullProgressMonitor()));
		assertEquals(1, getProfile(profileLoadedId).query(new InstallableUnitQuery("hello", new Version("1.0.0.1")), new Collector(), new NullProgressMonitor()).size());
	}

	public void testInstallFeaturePatchReverseOrder() {
		IInstallableUnit featureGroup2 = getIU(repo, "hello.feature.2.feature.group", "1.0.0");
		IInstallableUnit helloIU2 = getIU(repo, "hello", "1.0.2");
		IInstallableUnit featureJar2 = getIU(repo, "hello.feature.2.feature.jar", "1.0.0");

		ProfileChangeRequest installFeature2 = new ProfileChangeRequest(getProfile(profileLoadedId));
		installFeature2.addInstallableUnits(new IInstallableUnit[] {featureGroup2, helloIU2, featureJar2});
		installFeature2.setInstallableUnitInclusionRules(featureGroup2, PlannerHelper.createOptionalInclusionRule(featureGroup2));
		installFeature2.setInstallableUnitInclusionRules(helloIU2, PlannerHelper.createOptionalInclusionRule(helloIU2));
		installFeature2.setInstallableUnitInclusionRules(featureJar2, PlannerHelper.createOptionalInclusionRule(featureJar2));

		ProvisioningPlan feature2Plan = createPlanner().getProvisioningPlan(installFeature2, new ProvisioningContext(), null);
		assertOK("installation of feature2", createEngine().perform(getProfile(profileLoadedId), new DefaultPhaseSet(), feature2Plan.getOperands(), new ProvisioningContext(), new NullProgressMonitor()));
		assertEquals(1, getProfile(profileLoadedId).query(new InstallableUnitQuery("hello", new Version("1.0.2")), new Collector(), new NullProgressMonitor()).size());

		ProfileChangeRequest installFeature1 = new ProfileChangeRequest(getProfile(profileLoadedId));
		IInstallableUnit featureGroup = getIU(repo, "hello.feature.1.feature.group", "1.0.0");
		IInstallableUnit featureJar = getIU(repo, "hello.feature.1.feature.jar", "1.0.0");
		IInstallableUnit helloIU = getIU(repo, "hello", "1.0.0");
		IInstallableUnit patch = getIU(repo, "hello.patch.feature.group", "1.0.0");
		IInstallableUnit helloPatch = getIU(repo, "hello", "1.0.0.1");
		IInstallableUnit patchJar = getIU(repo, "hello.patch.feature.jar", "1.0.0");

		installFeature1.addInstallableUnits(new IInstallableUnit[] {featureGroup, featureJar, helloIU, patch, helloPatch, patchJar});

		installFeature1.setInstallableUnitInclusionRules(featureGroup, PlannerHelper.createOptionalInclusionRule(featureGroup));
		installFeature1.setInstallableUnitInclusionRules(featureJar, PlannerHelper.createOptionalInclusionRule(featureJar));
		installFeature1.setInstallableUnitInclusionRules(helloIU, PlannerHelper.createOptionalInclusionRule(helloIU));
		installFeature1.setInstallableUnitInclusionRules(patch, PlannerHelper.createOptionalInclusionRule(patch));
		installFeature1.setInstallableUnitInclusionRules(helloPatch, PlannerHelper.createOptionalInclusionRule(helloPatch));
		installFeature1.setInstallableUnitInclusionRules(patchJar, PlannerHelper.createOptionalInclusionRule(patchJar));

		ProvisioningPlan feature1Plan = createPlanner().getProvisioningPlan(installFeature1, new ProvisioningContext(), null);
		assertOK("installation of feature1 and patch", createEngine().perform(getProfile(profileLoadedId), new DefaultPhaseSet(), feature1Plan.getOperands(), new ProvisioningContext(), new NullProgressMonitor()));
		assertEquals(1, getProfile(profileLoadedId).query(new InstallableUnitQuery("hello", new Version("1.0.0.1")), new Collector(), new NullProgressMonitor()).size());
	}

	public void installTogether() {
		IInstallableUnit featureGroup2 = getIU(repo, "hello.feature.2.feature.group", "1.0.0");
		IInstallableUnit helloIU2 = getIU(repo, "hello", "1.0.2");
		IInstallableUnit featureJar2 = getIU(repo, "hello.feature.2.feature.jar", "1.0.0");

		IInstallableUnit featureGroup = getIU(repo, "hello.feature.1.feature.group", "1.0.0");
		IInstallableUnit featureJar = getIU(repo, "hello.feature.1.feature.jar", "1.0.0");
		IInstallableUnit helloIU = getIU(repo, "hello", "1.0.0");
		IInstallableUnit patch = getIU(repo, "hello.patch.feature.group", "1.0.0");
		IInstallableUnit helloPatch = getIU(repo, "hello", "1.0.0.1");
		IInstallableUnit patchJar = getIU(repo, "hello.patch.feature.jar", "1.0.0");

		ProfileChangeRequest installEverything = new ProfileChangeRequest(getProfile(profileLoadedId));
		installEverything.addInstallableUnits(new IInstallableUnit[] {featureGroup2, helloIU2, featureJar2, featureGroup, featureJar, helloIU, patch, helloPatch, patchJar});
		installEverything.setInstallableUnitInclusionRules(featureGroup2, PlannerHelper.createOptionalInclusionRule(featureGroup2));
		installEverything.setInstallableUnitInclusionRules(helloIU2, PlannerHelper.createOptionalInclusionRule(helloIU2));
		installEverything.setInstallableUnitInclusionRules(featureJar2, PlannerHelper.createOptionalInclusionRule(featureJar2));

		installEverything.setInstallableUnitInclusionRules(featureGroup, PlannerHelper.createOptionalInclusionRule(featureGroup));
		installEverything.setInstallableUnitInclusionRules(featureJar, PlannerHelper.createOptionalInclusionRule(featureJar));
		installEverything.setInstallableUnitInclusionRules(helloIU, PlannerHelper.createOptionalInclusionRule(helloIU));
		installEverything.setInstallableUnitInclusionRules(patch, PlannerHelper.createOptionalInclusionRule(patch));
		installEverything.setInstallableUnitInclusionRules(helloPatch, PlannerHelper.createOptionalInclusionRule(helloPatch));
		installEverything.setInstallableUnitInclusionRules(patchJar, PlannerHelper.createOptionalInclusionRule(patchJar));

		ProvisioningPlan plan = createPlanner().getProvisioningPlan(installEverything, new ProvisioningContext(), null);
		assertOK("installation of feature1 and patch", createEngine().perform(getProfile(profileLoadedId), new DefaultPhaseSet(), plan.getOperands(), new ProvisioningContext(), new NullProgressMonitor()));
		assertEquals(1, getProfile(profileLoadedId).query(new InstallableUnitQuery("hello", new Version("1.0.0.1")), new Collector(), new NullProgressMonitor()).size());
	}
}