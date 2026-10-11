import { UserCard } from "./UserCard";

test("renders name", () => {
  expect(UserCard({ name: "Ada" })).toBeTruthy();
});
