describe("login", () => {
  it("shows the form", () => {
    cy.visit("/login");
    cy.get("form").should("exist");
  });
});
