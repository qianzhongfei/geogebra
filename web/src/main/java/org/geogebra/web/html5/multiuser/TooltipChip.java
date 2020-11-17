package org.geogebra.web.html5.multiuser;

import org.geogebra.common.awt.GColor;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Label;

public class TooltipChip extends Label {

	public TooltipChip(String user, GColor color) {
		addStyleName("tooltipChip");
		setText(user);
		Style style = getElement().getStyle();
		style.setBackgroundColor(color.toString());
	}

	public void hide() {
		getElement().addClassName("invisible");
	}

	public void show(double x, double y) {
		getElement().removeClassName("invisible");
		Style style = getElement().getStyle();
		style.setLeft(x, Style.Unit.PX);
		style.setTop(y, Style.Unit.PX);
	}
}
