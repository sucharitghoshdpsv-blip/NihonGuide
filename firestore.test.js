const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      dailyBudgetJpy: 15000,
      mfaEnabled: false,
    })
  );
});

test("User can create and read their own profile and itinerary", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Traveler",
      dailyBudgetJpy: 18000,
      mfaEnabled: true,
      interests: ["Food", "Temples"],
      visitedRegions: ["Tokyo"],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("itineraries").doc("item_1").set({
      userId: ALICE_UID,
      dayNumber: 1,
      timeSlot: "09:00",
      title: "Senso-ji Temple",
      city: "Tokyo",
      category: "Temples",
      notes: "Arrive early",
      estimatedCostJpy: 500,
      completed: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("itineraries").get());
});

test("Cross-user access to private expenses and encrypted documents is denied", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).collection("documents").doc("passport_1").set({
      userId: BOB_UID,
      title: "Passport",
      docType: "Passport",
      encryptedPayload: "ciphertext==",
      ivBase64: "iv==",
      keyMode: "KEYSTORE",
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("documents").doc("passport_1").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("expenses").get());
});

test("Shadow update with extra field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      dailyBudgetJpy: 10000,
      mfaEnabled: false,
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Traveler posts enforce visibility filter on list and allow RSVP updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("traveler_posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      city: "Kyoto - Gion",
      topic: "Meetup",
      message: "Evening walk along Shirakawa canal!",
      meetupTime: "19:00",
      visibility: "public",
      interestedUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Unfiltered list fails
  await assertFails(bobDb.collection("traveler_posts").get());

  // Filtered list succeeds
  await assertSucceeds(
    bobDb.collection("traveler_posts").where("visibility", "==", "public").get()
  );

  // Bob can RSVP by updating interestedUids + updatedAt only
  await assertSucceeds(
    bobDb.collection("traveler_posts").doc("post_1").update({
      interestedUids: [ALICE_UID, BOB_UID],
      updatedAt: now,
    })
  );

  // Bob cannot edit Alice's message
  await assertFails(
    bobDb.collection("traveler_posts").doc("post_1").update({
      message: "Hacked",
      updatedAt: now,
    })
  );
});
