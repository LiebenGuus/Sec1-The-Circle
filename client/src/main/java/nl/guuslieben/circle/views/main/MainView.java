package nl.guuslieben.circle.views.main;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Optional;

import nl.guuslieben.circle.ClientService;
import nl.guuslieben.circle.common.User;
import nl.guuslieben.circle.common.UserData;
import nl.guuslieben.circle.common.util.CertificateUtilities;
import nl.guuslieben.circle.common.util.KeyUtilities;
import nl.guuslieben.circle.common.util.PasswordUtilities;
import nl.guuslieben.circle.views.MainLayout;

/**
 * A Designer generated component for the stub-tag template.
 *
 * Designer will add and remove fields with @Id mappings but does not overwrite
 * or otherwise change this file.
 */
@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle("The Circle")
@Tag("main-view")
@JsModule("./views/main-view.ts")
public class MainView extends LitTemplate {

    private final transient ClientService service;

    @Id
    private TextField name;
    @Id
    private TextField email;
    @Id
    private PasswordField password;

    @Id
    private Button sayHello;

    @Id
    private Paragraph key;

    public MainView(ClientService service) {
        this.service = service;
        this.sayHello.addClickListener(this::onClick);
    }

    public void onClick(ClickEvent<Button> event) {
        final UserData data = new UserData(this.name.getValue(), this.email.getValue());
        final Optional<X509Certificate> x509Certificate = this.service.csr(data, this.password.getValue());

        final PublicKey publicKey = this.service.getPair().getPublic();

        if (x509Certificate.isPresent()) {
            boolean validCertificate = CertificateUtilities.verify(x509Certificate.get(), this.service.getServerPublic());
            if (!validCertificate) {
                Notification.show("Server is not secure");
                return;
            }

            boolean valid = x509Certificate.get().getPublicKey().equals(publicKey);
            if (!valid) {
                Notification.show("Server is not secure");
                return;
            }
        } else {
            Notification.show("Could not collect certificate from server");
            return;
        }

        Notification.show("Server verified, registering user..");

        final String base64private = KeyUtilities.encodeKeyToBase64(this.service.getPair().getPrivate());
        final String password = this.password.getValue();

        final User user = new User(
                this.email.getValue(),
                this.name.getValue(),
                PasswordUtilities.encrypt(password, password, publicKey)
        );
        this.service.register(user);

        final String privateKey = PasswordUtilities.encrypt(base64private, password, publicKey);
        this.service.store(privateKey, this.email.getValue());
    }
}
