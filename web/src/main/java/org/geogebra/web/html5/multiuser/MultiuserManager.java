package org.geogebra.web.html5.multiuser;

import java.util.HashMap;

import org.geogebra.common.awt.GBasicStroke;
import org.geogebra.common.awt.GColor;
import org.geogebra.common.awt.GGraphics2D;
import org.geogebra.common.euclidian.EuclidianView;
import org.geogebra.common.factories.AwtFactory;
import org.geogebra.common.main.App;

public class MultiuserManager {

	public static final MultiuserManager INSTANCE = new MultiuserManager();

	private final HashMap<String, User> activeInteractions = new HashMap<>();

	private MultiuserManager() {
		// singleton class
	}

	public void addInteraction(App app, String user, GColor color, String label) {
		User currentUser = activeInteractions
				.computeIfAbsent(user, k -> new User(user, color));
		for (User u : activeInteractions.values()) {
			if (u != currentUser) {
				u.removeInteraction(label);
			}
		}
		currentUser.addInteraction(app.getActiveEuclidianView(), label);
	}

	public void paintInteractionBoxes(EuclidianView view, GGraphics2D graphics) {
		graphics.setStroke(AwtFactory.getPrototype()
				.newBasicStroke(5, GBasicStroke.CAP_ROUND, GBasicStroke.JOIN_ROUND));
		for (User user : activeInteractions.values()) {
			user.paintInteractionBoxes(view, graphics);
		}
	}
}
