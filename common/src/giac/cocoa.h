/* -*- mode:C++ ; compile-command: "g++-3.4 -Wall -I.. -g -c cocoa.cc" -*- */
/*  Copyright (C) 2000,2014 B. Parisse, Institut Fourier, 38402 St Martin d'Heres
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

#ifndef _GIAC_COCOA_H_
#define _GIAC_COCOA_H_
#include "first.h"
#include "gausspol.h"
#include "gen.h"

// special code for polynomial up to 11 variables (max deg<32768) 
//#define GROEBNER_VARS 11
#define GROEBNER_VARS 15

#ifndef NO_NAMESPACE_GIAC
namespace giac {
#endif // ndef NO_NAMESPACE_GIAC

  bool f5(vectpoly &,const gen & order);
  bool cocoa_gbasis(vectpoly &,const gen & order);
  vecteur cocoa_in_ideal(const vectpoly & g,const vectpoly & v,const gen & ordre);
  bool cocoa_greduce(const vectpoly & r,const vectpoly & v,const gen & order,vectpoly & res);

#ifndef CAS38_DISABLED
  // giac code for poly up to 15 variables
  bool gbasis8(const vectpoly & v,int & order,vectpoly & res,environment * env,bool modularcheck,bool & rur,GIAC_CONTEXT);
#endif

#ifndef NO_NAMESPACE_GIAC
} // namespace giac
#endif // NO_NAMESPACE_GIAC

#endif // _GIAC_COCOA_H
