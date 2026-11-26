const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const SELLER_UID = "seller_123";
const BUYER_UID = "buyer_456";
const OTHER_UID = "other_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validPropertyPayload(ownerId = SELLER_UID, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    ownerId,
    ownerName: "علی رضایی",
    title: "آپارتمان ۱۱۵ متری نوساز در سعادت‌آباد",
    description: "بسیار خوش‌نقشه، نورگیر عالی، فروش مستقیم توسط مالک بدون واسطه.",
    propertyType: "APARTMENT",
    status: "ACTIVE",
    city: "تهران",
    neighborhood: "سعادت‌آباد",
    priceToman: 12500000000,
    areaSqm: 115,
    bedrooms: 2,
    buildYear: 1402,
    floor: 3,
    hasParking: true,
    hasElevator: true,
    hasStorage: true,
    hasTitleDeed: true,
    coverPreset: "HERO_HOME",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("properties").where("status", "==", "ACTIVE").get());
  await assertFails(unauthDb.collection("properties").doc("prop_1").set(validPropertyPayload()));
  await assertFails(unauthDb.collection("conversations").where("participantUids", "array-contains", BUYER_UID).get());
  await assertFails(unauthDb.collection("users").doc(SELLER_UID).get());
});

test("User profile & favorites: strict owner-only isolation", async () => {
  const sellerDb = testEnv.authenticatedContext(SELLER_UID).firestore();
  const buyerDb = testEnv.authenticatedContext(BUYER_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    sellerDb.collection("users").doc(SELLER_UID).set({
      userId: SELLER_UID,
      displayName: "علی رضایی",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    sellerDb.collection("users").doc(SELLER_UID).update({
      displayName: "علی رضایی (مالک)",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user profile read/write must fail
  await assertFails(buyerDb.collection("users").doc(SELLER_UID).get());
  await assertFails(
    buyerDb.collection("users").doc(SELLER_UID).update({
      displayName: "هکر",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Favorites require existing property & owner match
  await assertSucceeds(
    sellerDb.collection("properties").doc("prop_fav").set(validPropertyPayload(SELLER_UID))
  );
  await assertSucceeds(
    buyerDb.collection("users").doc(BUYER_UID).collection("favorites").doc("prop_fav").set({
      userId: BUYER_UID,
      propertyId: "prop_fav",
      createdAt: now,
    })
  );
  await assertSucceeds(
    buyerDb.collection("users").doc(BUYER_UID).collection("favorites").get()
  );
  await assertFails(
    sellerDb.collection("users").doc(BUYER_UID).collection("favorites").get()
  );
  await assertFails(
    buyerDb.collection("users").doc(BUYER_UID).collection("favorites").doc("missing_prop").set({
      userId: BUYER_UID,
      propertyId: "missing_prop",
      createdAt: now,
    })
  );
});

test("Properties: valid create/update, query alignment, ghost fields, and terminal SOLD lock", async () => {
  const sellerDb = testEnv.authenticatedContext(SELLER_UID).firestore();
  const buyerDb = testEnv.authenticatedContext(BUYER_UID).firestore();

  // Valid create (no @DocumentId `id` key)
  await assertSucceeds(
    sellerDb.collection("properties").doc("prop_1").set(validPropertyPayload(SELLER_UID))
  );

  // Reject extra `id` field or ghost field
  await assertFails(
    sellerDb.collection("properties").doc("prop_2").set(validPropertyPayload(SELLER_UID, { id: "prop_2" }))
  );
  await assertFails(
    sellerDb.collection("properties").doc("prop_3").set(validPropertyPayload(SELLER_UID, { isVerified: true }))
  );
  // Reject future timestamp
  await assertFails(
    sellerDb.collection("properties").doc("prop_4").set(validPropertyPayload(SELLER_UID, { createdAt: new Date(Date.now() + 60000) }))
  );
  // Reject spoofing another ownerId
  await assertFails(
    buyerDb.collection("properties").doc("prop_5").set(validPropertyPayload(SELLER_UID))
  );

  // Filtered buyer query succeeds; unfiltered query fails
  await assertSucceeds(
    buyerDb.collection("properties").where("status", "==", "ACTIVE").get()
  );
  await assertSucceeds(
    sellerDb.collection("properties").where("ownerId", "==", SELLER_UID).get()
  );
  await assertFails(buyerDb.collection("properties").get());

  // Non-owner cannot update or delete
  await assertFails(
    buyerDb.collection("properties").doc("prop_1").update({
      priceToman: 1000,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(buyerDb.collection("properties").doc("prop_1").delete());

  // Owner can partially update active listing
  await assertSucceeds(
    sellerDb.collection("properties").doc("prop_1").update({
      priceToman: 12200000000,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Owner cannot mutate createdAt or ownerId
  await assertFails(
    sellerDb.collection("properties").doc("prop_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Owner marks listing SOLD (terminal state)
  await assertSucceeds(
    sellerDb.collection("properties").doc("prop_1").update({
      status: "SOLD",
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Once SOLD, further updates are blocked (terminal state locking)
  await assertFails(
    sellerDb.collection("properties").doc("prop_1").update({
      status: "ACTIVE",
      updatedAt: new Date(Date.now() - 100),
    })
  );
  // Buyer cannot read SOLD property
  await assertFails(buyerDb.collection("properties").doc("prop_1").get());
  // Owner can still read and delete their SOLD property
  await assertSucceeds(sellerDb.collection("properties").doc("prop_1").get());
  await assertSucceeds(sellerDb.collection("properties").doc("prop_1").delete());
});

test("Conversations and messages: buyer-seller direct chat isolation", async () => {
  const sellerDb = testEnv.authenticatedContext(SELLER_UID).firestore();
  const buyerDb = testEnv.authenticatedContext(BUYER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const now = new Date(Date.now() - 3000);

  await assertSucceeds(
    sellerDb.collection("properties").doc("prop_chat").set(validPropertyPayload(SELLER_UID))
  );

  const convId = `prop_chat_${BUYER_UID}`;
  const participants = [BUYER_UID, SELLER_UID];

  // Buyer initiates direct chat with seller
  await assertSucceeds(
    buyerDb.collection("conversations").doc(convId).set({
      propertyId: "prop_chat",
      propertyTitle: "آپارتمان ۱۱۵ متری نوساز در سعادت‌آباد",
      propertyCity: "تهران",
      propertyPriceToman: 12500000000,
      buyerId: BUYER_UID,
      buyerName: "مریم محمدی",
      sellerId: SELLER_UID,
      sellerName: "علی رضایی",
      participantUids: participants,
      lastMessage: "سلام، آیا امکان بازدید هست؟",
      lastSenderId: BUYER_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Self-chat (buyer == seller) must fail
  await assertFails(
    sellerDb.collection("conversations").doc(`prop_chat_${SELLER_UID}`).set({
      propertyId: "prop_chat",
      propertyTitle: "آپارتمان ۱۱۵ متری نوساز در سعادت‌آباد",
      propertyCity: "تهران",
      propertyPriceToman: 12500000000,
      buyerId: SELLER_UID,
      buyerName: "علی رضایی",
      sellerId: SELLER_UID,
      sellerName: "علی رضایی",
      participantUids: [SELLER_UID, SELLER_UID],
      lastMessage: "تست",
      lastSenderId: SELLER_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Empty messages query with participantUids filter succeeds for participant
  await assertSucceeds(
    buyerDb.collection("conversations").doc(convId).collection("messages")
      .where("participantUids", "array-contains", BUYER_UID).get()
  );
  // Unfiltered query on messages fails even for participant
  await assertFails(
    buyerDb.collection("conversations").doc(convId).collection("messages").get()
  );

  // Buyer sends a message & updates conversation summary
  await assertSucceeds(
    buyerDb.collection("conversations").doc(convId).collection("messages").doc("msg_1").set({
      conversationId: convId,
      senderId: BUYER_UID,
      senderName: "مریم محمدی",
      participantUids: participants,
      text: "سلام، آیا امکان بازدید هست؟",
      createdAt: new Date(Date.now() - 2000),
    })
  );

  // Seller lists conversations and replies
  await assertSucceeds(
    sellerDb.collection("conversations").where("participantUids", "array-contains", SELLER_UID).get()
  );
  await assertSucceeds(
    sellerDb.collection("conversations").doc(convId).collection("messages").doc("msg_2").set({
      conversationId: convId,
      senderId: SELLER_UID,
      senderName: "علی رضایی",
      participantUids: participants,
      text: "بله، فردا عصر ساعت ۵ در خدمت شما هستم.",
      createdAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    sellerDb.collection("conversations").doc(convId).update({
      lastMessage: "بله، فردا عصر ساعت ۵ در خدمت شما هستم.",
      lastSenderId: SELLER_UID,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Outsider cannot read conversation, query conversations unfiltered, or send message
  await assertFails(otherDb.collection("conversations").doc(convId).get());
  await assertFails(otherDb.collection("conversations").get());
  await assertFails(
    otherDb.collection("conversations").doc(convId).collection("messages").doc("msg_3").set({
      conversationId: convId,
      senderId: OTHER_UID,
      senderName: "غریبه",
      participantUids: [BUYER_UID, OTHER_UID],
      text: "مزاحمت",
      createdAt: new Date(Date.now() - 500),
    })
  );
});
