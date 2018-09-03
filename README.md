# slee-test-framework
Provide a consistent mocking and test driven approach to jain slee component development

    @BeforeEach
    public void setup() throws InstantiationException, IllegalAccessException, Exception {
        mockSlee = new MockSlee();

        mockSlee.add(NameVendorVersion.builder().name("mockit").vendor("mofokom").version("0.0.1").build(), Object.class);

        mockSbb = new MockSbb<>(CompleteExampleAnnotatedSbb.class);
        mockSimpleSbb = new MockSbb<>(SimpleExampleAnnotatedSbb.class,SimpleExampleSbbLocalObject.class);

        mockSlee.add(mockSbb);
        doReturn(mockSlee.mockChildRelation(mockSimpleSbb.getSbbLocalObject())).when(mockSbb.getSbb()).getChildRelation();
        mockSlee.start();
    }
