package org.geogebra.web.html5.multiuser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.geogebra.common.awt.GBasicStroke;
import org.geogebra.common.awt.GColor;
import org.geogebra.common.awt.GGraphics2D;
import org.geogebra.common.awt.GRectangle2D;
import org.geogebra.common.euclidian.Drawable;
import org.geogebra.common.euclidian.DrawableND;
import org.geogebra.common.euclidian.EuclidianView;
import org.geogebra.common.euclidian.GeneralPathClipped;
import org.geogebra.common.euclidian.draw.DrawLocus;
import org.geogebra.common.euclidian.draw.HasTransformation;
import org.geogebra.common.factories.AwtFactory;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.RectangleTransformable;
import org.geogebra.common.main.SelectionManager;
import org.geogebra.web.html5.main.AppW;
import org.gwtproject.timer.client.Timer;

class User {

	private final TooltipChip tooltip;
	private final Map<GeoElement, Timer> interactions;
	private final GColor color;

	User(String user, GColor color) {
		this.tooltip = new TooltipChip(user, color);
		this.interactions = new HashMap<>();
		this.color = color;
	}

	public void addInteraction(GeoElement geo) {
		AppW app = (AppW) geo.getKernel().getApplication();

		interactions.compute(geo, (k, v) -> {
			if (v == null) {
				v = new Timer() {
					@Override
					public void run() {
						interactions.remove(geo);
						app.getActiveEuclidianView().repaintView();
					}
				};
			}

			v.schedule(4000);
			return v;
		});

		app.getActiveEuclidianView().repaintView();
	}

	public void removeInteraction(GeoElement geo) {
		interactions.remove(geo);
	}

	public void paintInteractionBoxes(EuclidianView view, GGraphics2D graphics) {
		SelectionManager selection = view.getApplication().getSelectionManager();
		List<GeoElement> geos = interactions.keySet().stream()
				.filter((geo) -> !selection.containsSelectedGeo(geo))
				.collect(Collectors.toList());

		graphics.setColor(color);
		if (geos.size() == 0) {
			tooltip.hide();
		} else {
			showTooltipBy((AppW) view.getApplication(), geos.get(0));
		}

		if (geos.size() == 1) {
			GeoElement geo = geos.get(0);
			Drawable d = (Drawable) view.getDrawableFor(geo);
			if (d instanceof HasTransformation) {
				RectangleTransformable transformableGeo = (RectangleTransformable) geo;
				graphics.saveTransform();
				graphics.transform(((HasTransformation) d).getTransform());
				graphics.draw(AwtFactory.getPrototype().newRectangle(
						(int) transformableGeo.getWidth(),
						(int) transformableGeo.getHeight()
				));
				graphics.restoreTransform();
			} else if (d instanceof DrawLocus) {
				GBasicStroke current = graphics.getStroke();
				graphics.setStroke(AwtFactory.getPrototype()
						.newBasicStroke(geo.getLineThickness() + 2, GBasicStroke.CAP_ROUND,
								GBasicStroke.JOIN_ROUND));
				GeneralPathClipped gp = ((DrawLocus) d).getPath();
				graphics.draw(gp);
				graphics.setStroke(current);
			} else if (d != null) {
				graphics.draw(d.getBoundsForStylebarPosition());
			}
		} else if (geos.size() > 1) {
			graphics.draw(view.getEuclidianController().calculateBounds(geos));
		}
	}

	private void showTooltipBy(AppW app, GeoElement geo) {
		DrawableND drawable = app.getActiveEuclidianView().getDrawableFor(geo);
		if (drawable != null) {
			GRectangle2D bounds = drawable.getBoundsForStylebarPosition();
			double x = bounds.getMaxX() + getOffsetX(app);
			double y = bounds.getMinY() + getOffsetY(app);
			app.getAppletFrame().add(tooltip);
			tooltip.show(x, y);
		}
	}

	private double getOffsetY(AppW app) {
		return app.getActiveEuclidianView().getAbsoluteTop() - app.getAbsTop();
	}

	private double getOffsetX(AppW app) {
		return app.getActiveEuclidianView().getAbsoluteLeft() - app.getAbsLeft();
	}
}
