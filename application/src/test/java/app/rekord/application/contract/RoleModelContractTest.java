package app.rekord.application.contract;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.application.error.ErrorStatusTable;
import app.rekord.domain.shared.error.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * TASK-7.1 (P1-E01-T01): the role model the pinned rekord-contract carries. It reads the bundled spec the build reads
 * ({@code contract.spec}) as YAML, so it names no generated symbol and compiles against any contract version; against
 * a contract without the role model it fails on the missing schema, property or operation, never on a compile error.
 *
 * <p>Uniqueness of the roles is deliberately not a schema keyword ({@code uniqueItems} would need jackson-databind in
 * the generated code); the contract states it in the description and the server enforces it (TASK-8.5), so the
 * description sentence is what is tested here, and the absence of {@code uniqueItems} with it.
 */
class RoleModelContractTest {

    private static final String ROLE_REF = "#/components/schemas/Role";
    private static final String ROLES_PATH = "/org/members/{userId}/roles";
    private static final String MEMBER_PATH = "/org/members/{userId}";
    private static final String MEMBERS_PATH = "/org/members";
    private static final int CASE_INSENSITIVE_DOTALL = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;
    private static final Pattern VERSION = Pattern.compile("^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$");
    /** The status each error answer of setMemberRoles carries (UD-14.d). */
    private static final Map<String, Integer> ERROR_ANSWERS =
            Map.of("LAST_ADMIN", 400, "FORBIDDEN", 403, "NO_USER", 404, "VALIDATION_FAILED", 422);

    private static JsonNode spec;

    @BeforeAll
    static void readTheSpecTheBuildReads() throws IOException {
        String file = System.getProperty("contract.spec");
        assertThat(file).as("contract.spec system property").isNotBlank();
        spec = new YAMLMapper().readTree(Path.of(file).toFile());
    }

    // ---- AC 1: Role holds ADMIN, PLANNER and DJ; Me and UserAccount gain roles; InviteCreate.role accepts ADMIN ----

    @Test
    void role_holds_admin_planner_and_dj() {
        // Given / When
        List<String> values = strings(schema("Role").path("enum"));

        // Then
        assertThat(values).as("components.schemas.Role.enum").containsExactlyInAnyOrder("ADMIN", "PLANNER", "DJ");
    }

    @Test
    void me_gains_an_optional_roles_array_of_role_and_no_other_property_changes() {
        assertRolesAdded(
                "Me",
                List.of("id", "email", "display_name", "role", "roles", "organization_name"),
                List.of("id", "email", "display_name", "role"));
    }

    @Test
    void user_account_gains_an_optional_roles_array_of_role_and_no_other_property_changes() {
        assertRolesAdded(
                "UserAccount",
                List.of("id", "email", "display_name", "role", "roles", "status", "vendor_id", "last_login_at",
                        "created_at"),
                List.of("id", "email", "display_name", "role", "status", "created_at"));
    }

    @Test
    void invite_create_role_accepts_admin_and_no_other_property_of_invite_create_changes() {
        // Given
        JsonNode invite = schema("InviteCreate");
        JsonNode role = invite.path("properties").path("role");

        // Then: role is the Role enum (which now holds ADMIN), still defaulting to DJ, and nothing else moved
        assertThat(refOf(role)).as("InviteCreate.role").isEqualTo(ROLE_REF);
        assertThat(strings(schema("Role").path("enum"))).contains("ADMIN");
        assertThat(role.path("default").asText()).as("InviteCreate.role default").isEqualTo("DJ");
        assertThat(names(invite.path("properties")))
                .as("InviteCreate properties")
                .containsExactlyInAnyOrder("email", "display_name", "role", "vendor_id");
        assertThat(strings(invite.path("required"))).as("InviteCreate required").containsExactly("email");
    }

    // ---- AC 2: what role and roles state ----

    @Test
    void me_role_states_it_is_planner_when_the_member_holds_admin_or_planner_and_dj_otherwise() {
        assertRoleStatesTheCollapse("Me");
    }

    @Test
    void user_account_role_states_it_is_planner_when_the_member_holds_admin_or_planner_and_dj_otherwise() {
        assertRoleStatesTheCollapse("UserAccount");
    }

    @Test
    void me_roles_states_it_lists_every_active_role_sorted_by_name() {
        assertRolesStatesTheListing("Me");
    }

    @Test
    void user_account_roles_states_it_lists_every_active_role_sorted_by_name() {
        assertRolesStatesTheListing("UserAccount");
    }

    // ---- AC 3: the operation setMemberRoles ----

    @Test
    void set_member_roles_is_put_org_members_user_id_roles_next_to_the_other_member_operations() {
        // Given
        JsonNode path = present(spec.path("paths").path(ROLES_PATH), "path " + ROLES_PATH);
        JsonNode put = operation(ROLES_PATH, "put");
        JsonNode update = operation(MEMBER_PATH, "patch");

        // Then
        assertThat(names(path)).as("methods of " + ROLES_PATH).containsExactly("put");
        assertThat(put.path("operationId").asText()).isEqualTo("setMemberRoles");
        assertThat(put.path("tags")).as("tags, as updateMember has them").isEqualTo(update.path("tags"));
        assertThat(strings(put.path("tags"))).containsExactly("accounts");
        JsonNode userId = put.path("parameters").path(0);
        assertThat(userId.path("name").asText()).isEqualTo("userId");
        assertThat(userId.path("in").asText()).isEqualTo("path");
        assertThat(userId.path("required").asBoolean(false)).isTrue();
        assertThat(refOf(userId.path("schema"))).isEqualTo("#/components/schemas/Uuid");
    }

    @Test
    void set_member_roles_has_the_security_of_the_other_member_operations() {
        // Given
        JsonNode put = effectiveSecurity(operation(ROLES_PATH, "put"));

        // Then: non-empty (not public) and the same as listing, renaming and removing a member
        assertThat(put).as("security of PUT " + ROLES_PATH).isNotEmpty();
        assertThat(put).as("as listMembers").isEqualTo(effectiveSecurity(operation(MEMBERS_PATH, "get")));
        assertThat(put).as("as updateMember").isEqualTo(effectiveSecurity(operation(MEMBER_PATH, "patch")));
        assertThat(put).as("as deleteMember").isEqualTo(effectiveSecurity(operation(MEMBER_PATH, "delete")));
    }

    @Test
    void set_member_roles_takes_a_required_body_of_one_to_three_roles() {
        // Given
        JsonNode body = operation(ROLES_PATH, "put").path("requestBody");
        JsonNode schema = body.path("content").path("application/json").path("schema");
        JsonNode memberRoles = schema("MemberRoles");
        JsonNode roles = memberRoles.path("properties").path("roles");

        // Then
        assertThat(body.path("required").asBoolean(false)).as("requestBody required").isTrue();
        assertThat(refOf(schema)).isEqualTo("#/components/schemas/MemberRoles");
        assertThat(memberRoles.path("type").asText()).isEqualTo("object");
        assertThat(strings(memberRoles.path("required"))).as("MemberRoles required").containsExactly("roles");
        assertThat(names(memberRoles.path("properties"))).as("MemberRoles properties").containsExactly("roles");
        assertThat(roles.path("type").asText()).isEqualTo("array");
        assertThat(roles.path("minItems").asInt(-1)).as("minItems").isEqualTo(1);
        assertThat(roles.path("maxItems").asInt(-1)).as("maxItems").isEqualTo(3);
        assertThat(refOf(roles.path("items"))).as("items").isEqualTo(ROLE_REF);
    }

    @Test
    void member_roles_states_each_role_appears_at_most_once_and_a_repeat_is_refused_with_422() {
        // Given: no uniqueItems (the generator would need jackson-databind), so the description carries "unique"
        JsonNode memberRoles = schema("MemberRoles");
        String description = plain("MemberRoles description", memberRoles.path("description"));

        // Then
        assertThat(description).containsPattern(pattern("\\bat most once\\b"));
        assertThat(description)
                .containsPattern(pattern("\\b(twice|repeat\\w*|duplicate\\w*|more than once)\\b[^.]*\\b422 VALIDATION_FAILED\\b"));
        assertThat(memberRoles.path("properties").path("roles").has("uniqueItems"))
                .as("uniqueItems is deliberately absent")
                .isFalse();
    }

    @Test
    void set_member_roles_answers_200_user_list_and_every_error_through_the_common_error_response() {
        // Given
        JsonNode responses = operation(ROLES_PATH, "put").path("responses");

        // Then
        assertThat(names(responses)).as("response keys").containsExactlyInAnyOrder("200", "default");
        assertThat(refOf(responses.path("200").path("content").path("application/json").path("schema")))
                .as("200")
                .isEqualTo("#/components/schemas/UserList");
        assertThat(refOf(responses.path("default"))).as("default").isEqualTo("#/components/responses/Error");
        assertThat(refOf(spec.at("/components/responses/Error/content/application~1json/schema")))
                .as("the common Error response")
                .isEqualTo("#/components/schemas/Error");
    }

    @Test
    void the_four_error_codes_of_set_member_roles_exist_in_the_contract_and_the_domain_with_the_stated_statuses() {
        // Given
        List<String> contract = strings(schema("ErrorCode").path("enum"));

        for (Map.Entry<String, Integer> answer : ERROR_ANSWERS.entrySet()) {
            String code = answer.getKey();
            int status = answer.getValue();

            // Then: the contract lists the code, the domain keeps it, and the status table gives it the status
            assertThat(contract).as("ErrorCode enum of the contract").contains(code);
            assertThat(ErrorCode.valueOf(code)).as("domain ErrorCode").isNotNull();
            assertThat(ErrorStatusTable.rows())
                    .as("%s answers %d in ErrorStatusTable", code, status)
                    .anyMatch(row -> row.code().name().equals(code) && row.status() == status);
        }
    }

    @Test
    void set_member_roles_description_names_its_four_error_answers_by_status_and_code() {
        // Given: the answers are not separate responses, they all come through the default Error response
        String description = setMemberRolesDescription();

        for (Map.Entry<String, Integer> answer : ERROR_ANSWERS.entrySet()) {
            // Then
            assertThat(description)
                    .as("description of setMemberRoles names %d %s", answer.getValue(), answer.getKey())
                    .containsPattern(pattern("\\b" + answer.getValue() + " " + answer.getKey() + "\\b"));
        }
    }

    // ---- AC 4: what the description of setMemberRoles states ----

    @Test
    void set_member_roles_description_states_only_an_admin_may_call_it() {
        assertThat(setMemberRolesDescription()).containsPattern(pattern("\\bonly an admin\\b"));
    }

    @Test
    void set_member_roles_description_states_the_body_replaces_the_whole_role_set() {
        assertThat(setMemberRolesDescription()).containsPattern(pattern("\\bbody replaces\\b[^.]{0,60}\\brole set\\b"));
    }

    @Test
    void set_member_roles_description_states_an_empty_set_is_refused_with_422() {
        assertThat(setMemberRolesDescription())
                .containsPattern(pattern("\\bempty (set|list|array)\\b[^.]*\\b(refused|rejected)\\b[^.]*\\b422\\b"));
    }

    @Test
    void set_member_roles_description_states_taking_admin_from_the_last_active_admin_is_refused_with_400_last_admin() {
        assertThat(setMemberRolesDescription())
                .containsPattern(pattern(
                        "\\btaking ADMIN from the last active admin\\b[^.]*\\b(refused|rejected)\\b[^.]*\\b400 LAST_ADMIN\\b"));
    }

    @Test
    void set_member_roles_description_states_a_change_of_the_role_set_ends_every_session_of_the_member() {
        assertThat(setMemberRolesDescription())
                .containsPattern(pattern(
                        "\\b(change|changing)\\b[^.]*\\brole set\\b[^.]*\\b(ends|revokes)\\b[^.]*\\bevery session of the member\\b"));
    }

    // ---- AC 5: the spec the build reads is the release that carries all of this ----

    @Test
    void the_spec_the_build_reads_is_version_1_0_0_or_later() {
        // Given: v1.0.0 is the first release with ADMIN, roles and setMemberRoles
        String version = spec.path("info").path("version").asText();
        Matcher matcher = VERSION.matcher(version);

        // Then
        assertThat(matcher.matches()).as("info.version %s is MAJOR.MINOR.PATCH", version).isTrue();
        assertThat(Integer.parseInt(matcher.group(1))).as("major of info.version %s", version).isGreaterThanOrEqualTo(1);
    }

    // ---- helpers ----

    private static void assertRolesAdded(String name, List<String> properties, List<String> required) {
        // Given
        JsonNode schema = schema(name);
        JsonNode roles = schema.path("properties").path("roles");

        // Then: exactly the old keys plus roles, nothing newly required, roles an array of Role
        assertThat(names(schema.path("properties"))).as(name + " properties").containsExactlyInAnyOrderElementsOf(properties);
        assertThat(strings(schema.path("required"))).as(name + " required").containsExactlyInAnyOrderElementsOf(required);
        assertThat(strings(schema.path("required"))).as("roles is optional on " + name).doesNotContain("roles");
        assertThat(roles.path("type").asText()).as(name + ".roles type").isEqualTo("array");
        assertThat(refOf(roles.path("items"))).as(name + ".roles items").isEqualTo(ROLE_REF);
        assertThat(refOf(schema.path("properties").path("role"))).as(name + ".role").isEqualTo(ROLE_REF);
    }

    private static void assertRoleStatesTheCollapse(String name) {
        String description = plain(name + ".role description", schema(name).path("properties").path("role").path("description"));

        assertThat(description)
                .as(name + ".role description")
                .containsPattern(pattern("\\bPLANNER when the member holds ADMIN or PLANNER\\b.{0,12}\\bDJ otherwise\\b"));
    }

    private static void assertRolesStatesTheListing(String name) {
        String description = plain(name + ".roles description", schema(name).path("properties").path("roles").path("description"));

        // "organisation" in the contract; the ticket's wording said "business", either names the scope
        assertThat(description)
                .as(name + ".roles description")
                .containsPattern(pattern("\\bevery active role\\b[^.]*\\b(organi[sz]ation|business)\\b"))
                .containsPattern(pattern("\\bsorted by name\\W+ADMIN\\W+DJ\\W+PLANNER\\b"));
    }

    private static String setMemberRolesDescription() {
        return plain("description of PUT " + ROLES_PATH, operation(ROLES_PATH, "put").path("description"));
    }

    private static Pattern pattern(String regex) {
        return Pattern.compile(regex, CASE_INSENSITIVE_DOTALL);
    }

    /** Text with markdown code ticks dropped and whitespace folded, so a phrase is matched across lines and ticks. */
    private static String plain(String what, JsonNode description) {
        return present(description, what).asText("").replace("`", "").replaceAll("\\s+", " ").trim();
    }

    private static JsonNode effectiveSecurity(JsonNode operation) {
        return operation.has("security") ? operation.get("security") : spec.path("security");
    }

    private static JsonNode schema(String name) {
        return present(spec.path("components").path("schemas").path(name), "components.schemas." + name);
    }

    private static JsonNode operation(String path, String method) {
        return present(spec.path("paths").path(path).path(method), method.toUpperCase(Locale.ROOT) + " " + path);
    }

    private static JsonNode present(JsonNode node, String what) {
        assertThat(node.isMissingNode() || node.isNull()).as("%s is in the spec", what).isFalse();
        return node;
    }

    /** The {@code $ref} of a node, looking through the {@code allOf} wrapper that carries a description. */
    private static String refOf(JsonNode node) {
        if (node.has("$ref")) {
            return node.get("$ref").asText();
        }
        if (node.path("allOf").size() == 1 && node.path("allOf").path(0).has("$ref")) {
            return node.path("allOf").path(0).get("$ref").asText();
        }
        return "";
    }

    private static List<String> names(JsonNode object) {
        List<String> names = new ArrayList<>();
        object.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asText()));
        return values;
    }
}
